# TestOps report-uploader build and release

How to build and release the TestOps `report-uploader`, based on the verified May 19 2026 release thread plus the current repository workflows.

## Verified sources

- Slack thread: `C06QEJJE7UK` / `1779088098.844619`
- Workflows:
  - `.github/workflows/maven.yml`
  - `.github/workflows/release-prod.yml`
- Release evidence: GitHub release `v0.0.12`

## What this repo currently produces

There are two separate release outputs:

1. **JAR build** for CLI download/use
2. **Docker image** pushed to Docker Hub

They are related, but they are not the same workflow.

## Current build/release flow

### 1) Bump the version in the repo

- The version lives in `pom.xml` under `<version>`.
- Example from the source thread: the team bumped to `0.0.12` before releasing.

### 2) Merge the version/code change to `master`

- JAR workflow file: `.github/workflows/maven.yml`
- It runs on:
  - `push` to `master`
  - `pull_request` targeting `master`
- Build command: `mvn clean package`
- It uploads `target/*.jar` as a GitHub Actions artifact named `katalon-report-uploader`.

Important: **Build JAR is not a manual `workflow_dispatch` workflow**.

### 3) Publish the Docker image manually

- Manual workflow file: `.github/workflows/release-prod.yml`
- Workflow name in GitHub UI: **Build and push image to Docker Hub**
- Trigger: `workflow_dispatch`
- Optional input: `release_version` (example: `0.0.12`)

Required GitHub configuration:

- Secrets:
  - `DOCKER_USERNAME`
  - `DOCKER_PASSWORD`
- Repository variables:
  - `DOCKERHUB_ORG`
  - `DOCKERHUB_REPO`

Behavior:

- Logs into Docker Hub
- Builds multi-arch image for `linux/amd64`, `linux/arm/v7`, and `linux/arm64`
- Pushes tags derived from either:
  - the provided `release_version`, or
  - branch/SHA if no version is provided
- Also pushes `latest`

### 4) Give customers the JAR from GitHub Releases

- Verified public release page pattern: `https://github.com/katalon-studio/report-uploader/releases/tag/v<version>`
- Verified asset example: `katalon-report-uploader-0.0.12.jar`

In the source thread, the final guidance was to give customers the GitHub release download link.

## Operational notes from the May 19 2026 thread

### Functional change that prompted the release

- The uploader already received the TestOps execution URL from the upload API.
- The shipped fix was to print/log that URL after upload so users can see it.

### Real failure sequence seen during release

1. Docker publish initially failed because of a base image/runtime issue.
   - Fix landed in PR #53: `fix: replace missing Docker runtime base image`
2. Docker publish then failed at Docker Hub push with auth/scope errors.
   - Error seen: `insufficient_scope: authorization failed`
3. Team recovered by updating `DOCKER_PASSWORD` in GitHub Actions secrets.
   - The thread notes this was updated with a Docker access token.
4. A later run of **Build and push image to Docker Hub** succeeded.

## Practical checklist for the next release

1. Update `pom.xml` version.
2. Open and merge the PR to `master`.
3. Confirm `Build JAR` passed on `master`.
4. Run **Build and push image to Docker Hub** with `release_version=<new-version>`.
5. If Docker publish fails, check Docker Hub credentials first:
   - `DOCKER_USERNAME`
   - `DOCKER_PASSWORD`
6. Confirm the GitHub release/tag exists and that the JAR asset is attached.
7. Share the GitHub release link with docs/customer-facing teams.

## Known caveats

- The current workflows clearly cover JAR artifact build and Docker image publish.
- The repository does **not** currently show an obvious workflow that creates the GitHub release page itself, even though release `v0.0.12` exists. Treat GitHub Release creation/attachment as something to verify each time rather than assuming it is fully automated.
- If someone says they cannot manually trigger the JAR build, that matches the current workflow file: the JAR workflow is push/PR-based, not manual-dispatch-based.
