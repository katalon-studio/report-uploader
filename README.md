# Katalon Report Uploader

Katalon Report Uploader is a utility to upload reports to Katalon TestOps. At this moment it supports JUnit, Katalon Studio, and Katalon Recorder report format. It can be used with CLI, Docker, and Github Action.

## Companion products

### Katalon TestOps

[Katalon TestOps](https://analytics.katalon.com) is a web-based application that provides dynamic perspectives and an insightful look at your automation testing data. You can leverage your automation testing data by transforming and visualizing your data; analyzing test results; seamlessly integrating with such tools as Katalon Studio and Jira; maximizing the testing capacity with remote execution.

* Read our [documentation](https://docs.katalon.com/katalon-analytics/docs/overview.html).
* Ask a question on [Forum](https://forum.katalon.com/categories/katalon-analytics).
* Request a new feature on [GitHub](CONTRIBUTING.md).
* Vote for [Popular Feature Requests](https://github.com/katalon-analytics/katalon-analytics/issues?q=is%3Aopen+is%3Aissue+label%3Afeature-request+sort%3Areactions-%2B1-desc).
* File a bug in [GitHub Issues](https://github.com/katalon-analytics/katalon-analytics/issues).

### Katalon Studio
[Katalon Studio](https://www.katalon.com) is a free and complete automation testing solution for Web, Mobile, and API testing with modern methodologies (Data-Driven Testing, TDD/BDD, Page Object Model, etc.) as well as advanced integration (JIRA, qTest, Slack, CI, Katalon TestOps, etc.). Learn more about [Katalon Studio features](https://www.katalon.com/features/).

## Usage

Please see [Katalon TestOps documentation](https://docs.katalon.com/katalon-analytics/docs/project-management-import-cli.html).

## JUnit XML Merger

The report uploader includes a powerful JUnit XML merger functionality that can merge multiple JUnit XML files from subdirectories into a single consolidated XML file. This is particularly useful for handling test reruns and consolidating results from multiple test executions.

### Features

#### 🔄 **Timestamp-Based Precedence**
- **Smart Merging**: Uses `timestamp` attributes from `testsuite` elements to determine execution order
- **Latest Wins**: Later timestamps have higher priority when merging duplicate test cases
- **Fallback Logic**: If timestamps are equal or null, falls back to status-based priority (Pass > Error > Failure > Skipped)

#### 📊 **Comprehensive Statistics**
- **Accurate Counts**: Properly aggregates test counts, failure counts, error counts, and skip counts
- **Suite Name Preservation**: Keeps original suite name when all reports have the same name
- **Time Aggregation**: Combines execution times from all test cases

#### 🔍 **Advanced Test Case Handling**
- **Duplicate Resolution**: Intelligently handles duplicate test cases from reruns
- **Status Preservation**: Maintains test case status, error messages, and stack traces
- **Properties Management**: Preserves system properties from the first XML file

#### 🛡️ **Robust Processing**
- **Recursive Search**: Automatically finds all XML files in the specified directory and subdirectories
- **Error Handling**: Gracefully handles malformed XML files and continues processing
- **Detailed Logging**: Provides comprehensive logging of the merge process

### Usage

#### Command Line
```bash
# Using Maven
mvn compile exec:java -Dexec.mainClass="com.katalon.kit.report.uploader.UploaderApplication" -Dexec.args="merge-junit 'path/to/directory'"

# Using the provided shell script
./uploader.sh merge-junit "path/to/directory"
```

#### Example Directory Structure
```
test-rerun/20250710_193226/Traffic Agent/Verify Data Tracking On Support Elements/
├── original/
│   └── JUnit_Report.xml          # 9 tests, 2 failures
├── rerun1/
│   └── JUnit_Report.xml          # 2 tests, 1 failure (TC001 fixed)
└── rerun2/
    └── JUnit_Report.xml          # 1 test, 0 failures (TC009 fixed)
```

#### Execution
```bash
mvn compile exec:java -Dexec.mainClass="com.katalon.kit.report.uploader.UploaderApplication" -Dexec.args="merge-junit 'test-rerun/20250710_193226/Traffic Agent/Verify Data Tracking On Support Elements'"
```

```bash
./uploader.sh merge-junit "test-rerun/20250710_193226/Traffic Agent/Verify Data Tracking On Support Elements"
```

#### Output
Creates a merged file at:
```
test-rerun/20250710_193226/Traffic Agent/Verify Data Tracking On Support Elements/merged-junit-report.xml
```

### Sample Test Results

#### Real-World Test Scenario
**Original Run** (9 tests, 2 failures):
- TC001: FAILED (Expected 4 click action requests but found 3)
- TC002-TC008: PASSED
- TC009: FAILED (Detected duplicated timestamp)

**Rerun1** (2 tests, 1 failure):
- TC001: PASSED ✅ (Fixed the click request issue)
- TC009: FAILED (Still has duplicated timestamp issue)

**Rerun2** (1 test, 0 failures):
- TC009: PASSED ✅ (Fixed the timestamp duplication issue)

**Final Merged Result** (9 tests, 1 failure):
- TC001: PASSED (from rerun1 - later timestamp)
- TC002-TC008: PASSED (from original)
- TC009: FAILED (from rerun2 - latest timestamp, but still failed)

#### Mock Test Scenario
**Test Files with Timestamps**:
- `mock1.xml`: `2025-07-10T10:00:00.000Z` (earliest)
- `mock2.xml`: `2025-07-10T11:00:00.000Z`
- `mock3.xml`: `2025-07-10T12:00:00.000Z`
- `mock4.xml`: `2025-07-10T13:00:00.000Z`
- `mock5.xml`: `2025-07-10T14:00:00.000Z`
- `mock6.xml`: `2025-07-10T15:00:00.000Z` (latest)

**Final Merged Result** (8 tests, 1 failure, 1 error, 1 skip):
- All test cases from the latest timestamp (`2025-07-10T15:00:00.000Z`) are preserved
- Persistent failures, errors, and skips remain in the final result

### Timestamp Processing

The merger reads ISO format timestamps from `testsuite` elements:
```xml
<testsuite name="Test Suite" tests="9" failures="2" errors="0" time="245.247" timestamp="2025-07-10T12:32:30.787Z">
```

**Precedence Logic**:
1. **Primary**: Latest timestamp wins (most recent run)
2. **Fallback**: If timestamps are equal/null, uses status-based priority:
   - Pass > Error > Failure > Skipped
   - More detailed test cases preferred over less detailed ones

### Output Format

The merged XML file follows standard JUnit XML format:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<testsuites name="Suite Name" time="total_time" tests="total_tests" failures="total_failures" errors="total_errors">
  <properties>
    <!-- System properties from first file -->
  </properties>
  <testsuite name="Suite Name" tests="total_tests" failures="total_failures" errors="total_errors" skipped="total_skipped" time="total_time">
    <!-- All unique test cases with latest results -->
    <testcase classname="..." name="..." time="..." status="...">
      <!-- Error/failure details if applicable -->
    </testcase>
  </testsuite>
</testsuites>
```

### Testing

#### Mock Data Testing
Run the provided test script to verify merger functionality:
```bash
./junit-mock/merge-mock.sh
```

This tests the merger with various scenarios including:
- Mixed pass/fail/error/skip test cases
- Timestamp-based precedence
- Duplicate test case resolution
- Statistics aggregation

#### Real Data Testing
Test with actual JUnit XML files:
```bash
./uploader.sh merge-junit "path/to/your/test/reports"
```

### Error Handling

The merger gracefully handles:
- **Missing timestamps**: Falls back to status-based precedence
- **Malformed XML**: Logs error and continues processing other files
- **Empty directories**: Returns appropriate warning messages
- **Invalid file paths**: Provides clear error messages

### Logging

The merger provides detailed debug logging showing:
- Files being processed
- Timestamp comparisons
- Test case resolution decisions
- Final statistics

Example log output:
```
DEBUG - Preferring newer test case based on timestamp: 2025-07-10T12:37:48.853Z > 2025-07-10T12:32:30.787Z
DEBUG - Updated duplicate test case: TC009 with timestamp: 2025-07-10T12:37:48.853Z
INFO - Successfully merged 4 XML files into: merged-junit-report.xml
```
