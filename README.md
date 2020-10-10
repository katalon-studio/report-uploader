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

## CLI usage

Please see [Katalon TestOps documentation](https://docs.katalon.com/katalon-analytics/docs/project-management-import-cli.html).

## Docker usage

### Examples

### Environment variables

`SERVER`
The URL of Katalon TestOps. Default `https://analytics.katalon.com`.

`EMAIL`
The email registered for your Katalon account.

`PASSWORD`
The password used for signing in Katalon TestOps or an API Key.

`PROJECT_ID`
Your project ID in Katalon TestOps.

`TYPE`
One of the values including "katalon", "junit", or "katalon_recorder".

`REPORT_PATH`
The path of the report folder. The physical report folder should be mounted as a Docker volume first.

```
docker run -t --rm -v c:\users\alex\data\report-uploader\junit-report-sample:/katalon/report -e PASSWORD=<KATALON_API_KEY> -e PROJECT_ID=72642 -e TYPE=junit -e REPORT_PATH=/katalon/report katalonstudio/report-uploader:0.0.7.11
```

## Github Action usage

Marketplace Listing: https://github.com/marketplace/actions/katalon-report-uploader.

### Environment variables

Inputs can also be provided as environment variables.

`SERVER`
The URL of Katalon TestOps. Default `https://analytics.katalon.com`.

`EMAIL`
The email registered for your Katalon account. Not required if API Key is used instead of password.

`PASSWORD`
The password used for signing in Katalon TestOps or an API Key.

`PROJECT_ID`
Your project ID in Katalon TestOps.

`TYPE`
One of the values including "katalon", "junit", or "katalon_recorder".

`REPORT_PATH`
The path of the report folder.

### Examples

```yaml
  - name: Katalon Report Uploader
    uses: katalon-studio/report-uploader@v0.0.7.11
    env:
      PASSWORD: ${{ secrets.KATALON_API_KEY }}
      PROJECT_ID: 50236
      TYPE: junit
      REPORT_PATH: ${{ github.workspace }}/junit-report-sample
```
