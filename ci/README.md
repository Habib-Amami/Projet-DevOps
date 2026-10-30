# Local CI stack: Jenkins, SonarQube, and Nexus

This directory contains the first-run CI environment for the project. It is
designed to start from a clean machine: it does not mount `~/.jenkins`, does not
reuse the old `achat_jenkins_home` volume, and does not depend on host paths.

All persistent data is stored in Docker named volumes under the `achat-ci`
Compose project.

> **Do not run the Jenkins pipeline until SonarQube and Nexus are configured.**
>
> The root `Jenkinsfile` defaults `RUN_SONAR=true` and `PUBLISH_NEXUS=true`.
> Without the SonarQube token, the SonarQube URL, the Nexus credentials, and
> the `maven-snapshots` repository, the CI run will fail.

## What starts

| Service | URL | Purpose |
| --- | --- | --- |
| Jenkins | <http://localhost:8080> | Runs the pipeline |
| SonarQube | <http://localhost:9000> | Code analysis |
| Nexus | <http://localhost:8081> | Stores the Maven artifact |
| PostgreSQL | internal only | Database for SonarQube |

Jenkins, SonarQube, PostgreSQL, and Nexus are all required parts of this stack.
There are no optional CI services or Compose profiles.

Ports are bound to `127.0.0.1`, so they are not reachable from other machines.

Pinned base versions:

- Jenkins `2.580.1` with JDK 21
- SonarQube `26.10.0.133078-community`
- Nexus Repository Manager `3.97.3`
- PostgreSQL `16`

Jenkins includes:

- JDK 21
- Node.js 24
- Git, Bash, curl, jq, unzip, and OpenSSH
- Docker CLI with the buildx plugin
- AWS CLI v2 and kubectl (for the EKS deployment)
- The pipeline plugins used by the root `Jenkinsfile`

## Requirements

- Linux: Docker Engine with Compose v2
- Windows: Docker Desktop using **Linux containers**, with WSL 2 enabled
- At least 8 GB of RAM for the full stack; 16 GB is more comfortable
- Free host ports `8080`, `50000`, `9000`, and `8081`

The Jenkins container mounts `/var/run/docker.sock` so Testcontainers and the
Docker CLI work. It runs as root locally because that is the most reliable
choice on both Linux and Docker Desktop. This is a local-development setup;
never expose this Jenkins to an untrusted network.

## First start

If an old Jenkins container is still running, remove it first. It usually owns
ports `8080` and `50000`:

```bash
docker stop achat-jenkins-local
docker rm achat-jenkins-local
```

Optionally remove the old Jenkins data volume:

```bash
docker volume rm achat_jenkins_home
```

Then, from this directory:

```bash
cd ci
cp .env.example .env
```

On Windows PowerShell:

```powershell
cd ci
Copy-Item .env.example .env
```

Edit `.env` and change `SONAR_DB_PASSWORD` if you want. Then start the complete
CI stack:

```bash
docker compose up -d --build
docker compose ps
```

This starts Jenkins, SonarQube, its PostgreSQL database, and Nexus. SonarQube
and Nexus are required, not optional. Jenkins also declares them as dependencies,
so targeting Jenkins alone still starts the rest of the CI stack.

Open http://localhost:8080. The required plugins are already baked into the
image. Complete the first-run wizard and create the Jenkins administrator
account.

If you need Jenkins' initial unlock password:

```bash
docker compose exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

SonarQube can take a minute or two to become fully ready even though its
container starts immediately.

## Mandatory CI setup

Complete these two sections before creating or running the pipeline job. The
pipeline is not considered ready until the checklist at the end of this section
passes.

### 1. Configure SonarQube

Wait until SonarQube is ready:

```bash
docker compose logs -f sonarqube
```

Or check:

```bash
curl -s http://localhost:9000/api/system/status
```

Open http://localhost:9000. The default credentials are `admin` / `admin`; change
the password when prompted.

Create a token:

1. In SonarQube, open **My Account** -> **Security**.
2. Generate a **User Token** named `jenkins`.
3. Copy the token.

Create the Jenkins credential:

1. In Jenkins, open **Manage Jenkins** -> **Credentials** -> **System** ->
   **Global credentials** -> **Add Credentials**.
2. Choose **Secret text**.
3. Set **ID** to `sonar-token`.
4. Paste the SonarQube token.

When you run the Jenkins job, set the `SONAR_HOST_URL` parameter to:

```text
SONAR_HOST_URL=http://sonarqube:9000
```

Do not use the `host.docker.internal` default from the Jenkinsfile with this
Compose stack.

#### SonarQube on Linux

SonarQube needs a higher `vm.max_map_count`:

```bash
sudo sysctl -w vm.max_map_count=524288
```

To persist it:

```bash
echo 'vm.max_map_count=524288' | sudo tee /etc/sysctl.d/99-sonarqube.conf
sudo sysctl --system
```

#### SonarQube on Windows Docker Desktop

Give Docker Desktop enough memory, then raise the setting inside its Linux VM.
In PowerShell:

```powershell
wsl -d docker-desktop sysctl -w vm.max_map_count=524288
```

If that command is unavailable, run:

```powershell
docker run --rm --privileged --pid=host alpine nsenter -t 1 -m -u -i -n -p -- sysctl -w vm.max_map_count=524288
```

This setting may reset when Docker Desktop restarts. Jenkins itself does not
need it; only SonarQube does.

### 2. Configure Nexus

Wait until Nexus is ready:

```bash
docker compose logs -f nexus
```

Open http://localhost:8081. Get the initial admin password:

```bash
docker compose exec nexus cat /nexus-data/admin.password
```

Log in as `admin`, change the password, and verify that a hosted Maven repository
named `maven-snapshots` exists. If it does not, create a **maven2 (hosted)**
repository with:

- Name: `maven-snapshots`
- Version policy: `Snapshot`
- Deployment policy: `Allow redeploy`

Also verify or create a **maven2 (hosted)** repository with:

- Name: `maven-releases`
- Version policy: `Release`
- Deployment policy: `Allow redeploy`

For this demo, `PUBLISH TO NEXUS` uploads the same built JAR as
`0.0.1-SNAPSHOT` to `maven-snapshots` and as `0.0.1` to `maven-releases`.
Allow redeploy on both repositories so rerunning CI can overwrite those
coordinates. The JAR's embedded Maven version remains `0.0.1-SNAPSHOT`.

Create the Jenkins credential:

1. Open **Manage Jenkins** -> **Credentials** -> **System** ->
   **Global credentials** -> **Add Credentials**.
2. Choose **Username with password**.
3. Set **ID** to `nexus-credentials`.
4. Use the Nexus username and password.

When you run the Jenkins job, set the `NEXUS_HOST` parameter to:

```text
NEXUS_HOST=nexus:8081
```

`NEXUS_HOST` must not include `http://`; the pipeline plugin adds the protocol.

### CI readiness checklist

Do not run the pipeline until all of these are true:

- [ ] SonarQube is reachable at http://localhost:9000 and reports `UP`
- [ ] Jenkins has a Secret text credential with ID `sonar-token`
- [ ] Nexus is reachable at http://localhost:8081
- [ ] Jenkins has a Username with password credential with ID `nexus-credentials`
- [ ] Nexus has the `maven-snapshots` repository
- [ ] Nexus has the `maven-releases` repository; both repositories allow redeploy
- [ ] Jenkins can run Docker commands through `/var/run/docker.sock`

## Create the pipeline job

In Jenkins:

1. Select **New Item**.
2. Enter `achat-pipeline`, select **Pipeline**, and confirm.
3. Under **Pipeline**, select **Pipeline script from SCM**.
4. Use:
   - SCM: Git
   - Repository URL: `https://github.com/Habib-Amami/igl5-g7-achat.git`
   - Branch specifier: `*/Asma/terraform` (use `*/develop` once the branch is merged)
   - Script path: `Jenkinsfile`
5. Save the job.

The **Branch specifier** decides which `Jenkinsfile` (which stages) Jenkins
loads. The `BRANCH` parameter decides which code is built. The Docker and EKS
stages exist only in a `Jenkinsfile` that contains them, so both must point to
a branch that has them.

> Jenkins reads the code from **GitHub**, not from your PC. Commit and push
> before every build, otherwise Jenkins builds the old code.

## Run the CI (build, test, Sonar, Nexus, Docker Hub)

Open the job and click **Build with Parameters**:

| Parameter | Value |
| --- | --- |
| `BRANCH` | `Asma/terraform` |
| `RUN_SONAR` | checked |
| `PUBLISH_NEXUS` | checked |
| `SONAR_HOST_URL` | `http://sonarqube:9000` (**not** the `host.docker.internal` default) |
| `NEXUS_HOST` | `nexus:8081` |
| `DOCKER_PUSH` | checked |
| `DOCKERHUB_USER` | your Docker Hub username |
| `DEPLOY_K8S` | unchecked for the first run (see the next section) |
| `EKS_CLUSTER` | `achat-eks` |
| `AWS_REGION` | `us-east-1` |
| `EMAIL_TO` | recipient address(es), separated by commas; leave empty to skip email |

Stages and what to expect:

| Stage | What it does | Where to check |
| --- | --- | --- |
| `CHECKOUT GIT` | Clones `BRANCH` from GitHub | Console log |
| `MVN CLEAN` / `ARTIFACT CONSTRUCTION` | Builds the JAR | Build artifacts |
| `UNIT TESTS` | 75 tests; the context test starts MySQL with Testcontainers | **Test Result** page |
| `MVN SONARQUBE` | Code analysis | http://localhost:9000, project `achat` |
| `PUBLISH TO NEXUS` | Uploads the JAR as snapshot and release | http://localhost:8081, `maven-snapshots` and `maven-releases` |
| `DOCKER BUILD` | Builds `<user>/achat-backend` and `<user>/achat-frontend`, tagged with the build number and `latest` | `docker images` |
| `DOCKER PUSH` | Pushes both images | hub.docker.com, your repositories |
| `DEPLOY TO EKS` | Deploys the app, Grafana LGTM, and Alloy (only with `DEPLOY_K8S`) | App URL and Grafana/Swagger port-forward commands printed at the end of the log |

Do not use `RUN_SONAR=false` or `PUBLISH_NEXUS=false` as a substitute for the
mandatory setup in this project.

### Build time

- The first build is slow: Maven and npm download dependencies, and Docker
  builds every image layer.
- Later builds are much faster: Maven dependencies stay in the Jenkins volume,
  Docker reuses unchanged layers (only your code is recompiled when `pom.xml`
  and `package-lock.json` did not change), and Docker Hub only receives the
  layers that changed.
- `UNIT TESTS` always starts a real MySQL container, so it keeps taking a few
  minutes. Stop the local application stack (`docker compose down` from the
  repository root) while Jenkins runs to free memory.

## Email notifications with Gmail

The pipeline sends one plain-text notification after every build, including
failed or aborted builds, when `EMAIL_TO` is set. The message contains the build
result, branch, and links to the build and console. Email delivery errors are
logged without changing the original CI result. Leaving `EMAIL_TO` empty skips
notifications, so SMTP setup is optional until you want to test it.

### Install the email plugin

`email-ext` is included in `plugins.txt`. From `ci/`, rebuild and recreate Jenkins:

```bash
docker compose up -d --build --no-deps jenkins
```

Wait for Jenkins to restart, then check **Manage Jenkins -> Plugins -> Installed**
for **Email Extension**. The existing Jenkins named volume keeps jobs and credentials.

### Configure Gmail SMTP

1. Enable **2-Step Verification** on the Gmail account that will send mail.
2. Create a Google **App password** for Jenkins. If app passwords are unavailable,
   check your account or organization restrictions. Use the app password for SMTP.
3. In Jenkins, create a **Username with password** credential with ID `gmail-smtp`:
   username = the full Gmail address; password = the Google app password.
4. Open **Manage Jenkins -> System -> Extended E-mail Notification** and configure:

   | Setting | Value |
   | --- | --- |
   | SMTP server | `smtp.gmail.com` |
   | SMTP port | `587` |
   | Credentials / SMTP authentication | `gmail-smtp` |
   | Use TLS / STARTTLS | enabled |
   | Use SSL | disabled |

5. Under **Jenkins Location**, set **System Admin e-mail address** to the sending
   Gmail address and **Jenkins URL** to the address you use to open Jenkins
   (for example `http://localhost:8080/`). Save the configuration.

Keep the app password in Jenkins credentials; it is not a pipeline parameter
and does not belong in `.env` or Git. The recipient is supplied through `EMAIL_TO`.

References: [Google app passwords](https://support.google.com/accounts/answer/185833),
[Gmail SMTP](https://developers.google.com/workspace/gmail/imap/imap-smtp), and
[Jenkins Email Extension](https://plugins.jenkins.io/email-ext/).

### Test email delivery first

To verify SMTP without running the application CI, create a temporary Jenkins
**Pipeline** job, choose **Pipeline script**, and paste:

```groovy
pipeline {
    agent any
    parameters {
        string(name: 'EMAIL_TO', defaultValue: '', description: 'Test recipient')
    }
    stages {
        stage('TEST EMAIL') {
            steps {
                script {
                    if (!params.EMAIL_TO?.trim()) {
                        error 'Set EMAIL_TO and run Build with Parameters.'
                    }
                    emailext(
                        to: params.EMAIL_TO.trim(),
                        subject: "Jenkins email test #${env.BUILD_NUMBER}",
                        body: "Gmail SMTP is working. Build: ${env.BUILD_URL}",
                        mimeType: 'text/plain'
                    )
                }
            }
        }
    }
}
```

Save and run the job once to register its parameter, then use **Build with
Parameters** and enter `EMAIL_TO`. Check the inbox and spam folder. This test job
does not suppress email errors, so authentication or connection problems are
visible in its console and build result.

After that succeeds, configure the application job to load the updated
`Jenkinsfile` from your pushed feature branch and supply `EMAIL_TO`. Its SCM
branch specifier selects the pipeline file; its `BRANCH` parameter selects the
application source to build. Set both to the branch you intend to test.

## Continuous delivery: Docker Hub and EKS

Terraform creates the cluster; Jenkins deploys onto it. Create the cluster
first with [`../terraform`](../terraform/README.md) (`terraform apply`), then
come back here.

The deployment includes observability. See
[`../observability/README.md`](../observability/README.md) for Grafana access,
example queries, and a monitoring-only deploy onto an existing application.

### 1. Rebuild Jenkins once

The Jenkins image needs the AWS CLI and kubectl:

```bash
docker compose up -d --build jenkins
```

The kubectl version is set by `KUBECTL_VERSION` in [`Dockerfile`](Dockerfile).
It must be within one minor version of the cluster (`terraform output
cluster_version`). Change it and rebuild if they differ.

### 2. Create a Docker Hub access token

1. Log in to https://app.docker.com.
2. Click your avatar -> **Account settings** -> **Personal access tokens**.
3. **Generate new token**: description `jenkins`, permissions **Read & Write**.
4. Copy the token (`dckr_pat_...`). It is shown only once.

### 3. Create the Jenkins credentials

**Manage Jenkins** -> **Credentials** -> **System** ->
**Global credentials (unrestricted)** -> **Add Credentials**. The ID must match
exactly.

| ID | Kind | Value | Needed for |
| --- | --- | --- | --- |
| `dockerhub-credentials` | Username with password | Username: your Docker Hub user. Password: the `dckr_pat_...` token | `DOCKER_PUSH` |
| `aws-learner-lab` | Secret file | A file with the Learner Lab **AWS Details -> AWS CLI** block (`[default]` + 3 lines), for example your `~/.aws/credentials` | `DEPLOY_K8S` |
| `achat-db-credentials` | Username with password | Username `achat_user`, password: a random string | `DEPLOY_K8S` |
| `achat-mysql-root` | Secret text | Another random string (MySQL root password) | `DEPLOY_K8S` |

> **`aws-learner-lab` expires with every lab session.** After each Start Lab,
> open the credential, click **Update**, and upload the new file. Otherwise the
> deploy fails with `ExpiredToken`.

### 4. Run the deployment

**Build with Parameters** with the same values as above, plus:

```text
DOCKER_PUSH=true
DEPLOY_K8S=true
```

The end of the `DEPLOY TO EKS` log shows the pods and:

```text
Application: http://xxxxxxxx.us-east-1.elb.amazonaws.com
```

Open that URL (it can take 2-3 minutes to answer). Each build deploys images
tagged with its build number, so the cluster always runs the code that was just
tested.

### 5. After the demo

Delete the application and the cluster, as described in
[`../terraform/README.md`](../terraform/README.md#step-5---clean-up-after-every-demo).
Leave `DEPLOY_K8S` unchecked while no cluster exists.

## Common first-run failures

### `Could not find credentials entry with ID 'sonar-token'`

The SonarQube credential has not been created. Create a **Secret text**
credential with ID `sonar-token` as described in the SonarQube section.

### SonarQube connection failure

The Jenkinsfile default is:

```text
http://host.docker.internal:9000
```

That address is not valid for this Compose network. Set the job parameter to:

```text
SONAR_HOST_URL=http://sonarqube:9000
```

### `Could not find credentials entry with ID 'nexus-credentials'`

Create a **Username with password** credential with ID `nexus-credentials` as
described in the Nexus section.

### Nexus repository not found

Create the hosted Maven repository named `maven-snapshots` with snapshot
version policy and redeploy enabled.

The pipeline runs SonarQube before Nexus. If the SonarQube stage fails, the
Nexus stage is skipped. Configure both before rerunning the job.

### SonarQube keeps restarting: `database "sonar" does not exist`

The PostgreSQL volume was initialized without the database (usually an
interrupted first start). Create it, then restart SonarQube:

```bash
docker compose exec sonarqube-db psql -U sonar -d postgres -c "CREATE DATABASE sonar OWNER sonar"
docker compose restart sonarqube
```

The next start runs the database migrations and can take several minutes.
`curl -s http://localhost:9000/api/system/status` shows `UP` when it is ready.

### `UNIT TESTS`: `Could not connect to Ryuk at 172.17.0.1`

Testcontainers runs inside the Jenkins container and must reach the test
containers through the host. `compose.yaml` sets
`TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal` for this. If you see the
error, recreate Jenkins so it picks up the setting:

```bash
docker compose up -d jenkins
```

### `UNIT TESTS`: `Can't get Docker image` or startup timeouts

The machine is short on memory or the image download is slow. Stop the local
application stack (`docker compose down` from the repository root) and pull the
test image once:

```bash
docker pull mysql:8.4
```

### `DOCKER BUILD`: `DOCKERHUB_USER est obligatoire`

Fill in the `DOCKERHUB_USER` parameter, or uncheck `DOCKER_PUSH`.

### `DOCKER PUSH`: `unauthorized` or `denied`

The `dockerhub-credentials` username or token is wrong, or the token is
read-only. Create a **Read & Write** token and update the credential.

### `DEPLOY TO EKS`: `ExpiredToken` or `voc-cancel-cred`

The lab session changed. Start the lab, copy the new AWS CLI block, and update
the `aws-learner-lab` secret file.

### `DEPLOY TO EKS`: `ResourceNotFoundException` or `no such host`

The cluster does not exist (destroyed, or removed by the lab). Run
`terraform apply` in [`../terraform`](../terraform/README.md) first.

## Stop, reset, and data

Stop everything but keep data:

```bash
docker compose down
```

Delete all CI data and start completely fresh:

```bash
docker compose down -v
```

`down -v` deletes the Jenkins home, SonarQube data, PostgreSQL data, and Nexus
data. Use it only when you intend to wipe the CI stack.

## Notes for Windows and Linux

- Windows must use Docker Desktop with Linux containers, not Windows containers.
- No host bind mounts are used for application data, so Windows paths do not
  appear in the Compose file.
- `docker compose` commands are the same on Linux and Windows.
- The Docker socket mount works with Docker Desktop and with a normal Linux
  Docker Engine.
- The application stack uses ports `3000`, `4200`, `4317`, `4318`, and `8089`.
  The CI stack uses `8080`, `50000`, `9000`, and `8081`, so the two stacks do
  not conflict.
- If port `8080` or `50000` is already in use, the usual cause is the old
  `achat-jenkins-local` container. Stop and remove it before starting this
  stack. Alternatively, change `JENKINS_PORT` or `JENKINS_AGENT_PORT` in
  `.env`.
