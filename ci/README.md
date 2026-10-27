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
| Jenkins | http://localhost:8080 | Runs the pipeline |
| SonarQube | http://localhost:9000 | Code analysis |
| Nexus | http://localhost:8081 | Stores the Maven artifact |
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
- Docker CLI
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
- [ ] Jenkins can run Docker commands through `/var/run/docker.sock`

## Create the pipeline job

In Jenkins:

1. Select **New Item**.
2. Enter `achat-ci`, select **Pipeline**, and confirm.
3. Under **Pipeline**, select **Pipeline script from SCM**.
4. Use:
   - SCM: Git
   - Repository URL: `https://github.com/Habib-Amami/Projet-DevOps.git`
   - Branch specifier: `*/develop`
   - Script path: `Jenkinsfile`
5. Save the job.

The job loads the `Jenkinsfile` from `develop`, while the `BRANCH` parameter selects
which branch is built. This keeps the CI definition stable even when the branch
being built changes.

The merged `Jenkinsfile` defines these parameters:

- `BRANCH`
- `RUN_SONAR`
- `PUBLISH_NEXUS`
- `SONAR_HOST_URL`
- `NEXUS_HOST`

## Run the CI

The full CI run requires both SonarQube and Nexus setup to be complete:

```text
BRANCH=develop
RUN_SONAR=true
PUBLISH_NEXUS=true
SONAR_HOST_URL=http://sonarqube:9000
NEXUS_HOST=nexus:8081
```

Do not use `RUN_SONAR=false` or `PUBLISH_NEXUS=false` as a substitute for the
mandatory setup in this project. Those parameters exist in the Jenkinsfile, but
the documented CI flow keeps both stages enabled.

To test a feature branch, first push that branch, then replace `BRANCH` with its
name. Jenkins checks out the selected branch from GitHub, not from your local
working tree. A local branch that has not been pushed is not visible to Jenkins.

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
