# report-uploader

Usage: https://docs.katalon.com/katalon-analytics/docs/project-management-import-cli.html.

# [GITHUB ACTION] report-uploader

## Inputs

### `api-key`

**Required** The API key in Katalon TestOps

### `project-id`

**Required** Your project ID in Katalon TestOps.

### `report-type`

**Required** One of the values including "katalon", "junit", or "katalon_recorder". 

### `report-path`

**Required** The path of report folder.

### `server-url`

**Optional** The URL of Katalon TestOps. Default `https://analytics.katalon.com`.

## Environment

### `TESTOPS_SERVER_URL`
The URL of Katalon TestOps. Default `https://analytics.katalon.com`

### `TESTOPS_EMAIL`
The email registered for your Katalon account.

### `TESTOPS_PASSWORD`
The password used for signing in Katalon TestOps or an API Key.

### `TESTOPS_PROJECT_ID`
Your project ID in Katalon TestOps.

### `TESTOPS_REPORT_TYPE`
One of the values including "katalon", "junit", or "katalon_recorder".

### `TESTOPS_REPORT_PATH`
The path of report folder.


## Example
```
  - name: Katalon Report Uploader
    uses: katalon-studio/report-uploader@v0.0.7.1
    with:
      api-key: ${{ secrets.TESTOPS_API_KEY}}
      project-id: 50236
      report-type: junit
      report-path: ${{ github.workspace }}/junit-report-sample
```

or

```
  - name: Katalon Report Uploader
    uses: katalon-studio/report-uploader@v0.0.7.1
    env:
      TESTOPS_EMAIL: quile@kms-technology.com
      TESTOPS_PASSWORD: 12345678
      TESTOPS_PROJECT_ID: 50236
      TESTOPS_REPORT_TYPE: junit
      TESTOPS_REPORT_PATH: ${{ github.workspace }}/junit-report-sample
```

# Companion products

##Katalon TestOps

[Katalon TestOps](https://analytics.katalon.com) is a web-based application that provides dynamic perspectives and an insightful look at your automation testing data. You can leverage your automation testing data by transforming and visualizing your data; analyzing test results; seamlessly integrating with such tools as Katalon Studio and Jira; maximizing the testing capacity with remote execution.

* Read our [documentation](https://docs.katalon.com/katalon-analytics/docs/overview.html).
* Ask a question on [Forum](https://forum.katalon.com/categories/katalon-analytics).
* Request a new feature on [GitHub](CONTRIBUTING.md).
* Vote for [Popular Feature Requests](https://github.com/katalon-analytics/katalon-analytics/issues?q=is%3Aopen+is%3Aissue+label%3Afeature-request+sort%3Areactions-%2B1-desc).
* File a bug in [GitHub Issues](https://github.com/katalon-analytics/katalon-analytics/issues).

## Katalon Studio
[Katalon Studio](https://www.katalon.com) is a free and complete automation testing solution for Web, Mobile, and API testing with modern methodologies (Data-Driven Testing, TDD/BDD, Page Object Model, etc.) as well as advanced integration (JIRA, qTest, Slack, CI, Katalon TestOps, etc.). Learn more about [Katalon Studio features](https://www.katalon.com/features/).
