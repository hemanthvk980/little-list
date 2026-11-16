# CI/CD, Helm and environment configuration

Repository: https://github.com/hemanthvk980/little-list

## Delivery flow

Pull request → GitHub Actions verification → merge to main → verification → publish two GHCR images tagged with the complete commit SHA → manually run Deploy Kubernetes for dev/staging/production.

Verification includes Java/PostgreSQL tests, Next build, capture/extension/QR tests, Helm lint/render for all environments, local Mailpit HTTP smoke, dependency audit and container builds. Publishing runs only after successful main-branch verification. PRs get no package or deployment credentials. Images use commit tags, not latest; restrict package write permissions to keep released tags unchanged. Digest pinning and image signing are future hardening work.

The deployment workflow checks that the selected SHA had a successful main-branch verification/publication run, checks out that SHA's chart, then deploys the same image tags. No Kubernetes credentials, namespaces, ingress controller, database or production accounts have been provisioned by these files. Deployment is not activated merely by pushing this repo.

## Environment split

| Setting | Location | Example |
| --- | --- | --- |
| Local development | Compose + Spring local profile | Local demo users, PostgreSQL, Mailpit |
| Non-secret defaults | deploy/helm/little-list/values.yaml | Ports, resources, Google profile |
| Per-environment overrides | deploy/environments/dev.yaml, staging.yaml, production.yaml | HTTPS host, TLS secret name |
| Runtime secrets | Existing Kubernetes Secret in each namespace | Database/SMTP/OAuth credentials |
| Deployment access | GitHub Environment secret KUBE_CONFIG | Namespace-scoped kubeconfig, separate per environment |
| Public hostname | GitHub Environment variable PUBLIC_HOST | registry.your-domain.in, no scheme or slash |
| Version to deploy | Workflow commit input | Full successful Git commit SHA |

Use namespaces little-list-dev, little-list-staging and little-list-production, with distinct databases, OAuth registrations, SMTP credentials and secrets. All hosted environments use Spring profile google; do not combine it with local. The chart refuses the exact local profile; operators must not introduce combined local profiles. Production cookies stay secure. Do not place secrets into values YAML or commit a real .env file. Spring consumes environment variables; it does not automatically load .env.

Create `little-list-secrets` outside Helm in each namespace using your secret manager/External Secrets controller or an administrator's secure provisioning process. Required keys: DATABASE_URL (JDBC PostgreSQL URL), DATABASE_USER, DATABASE_PASSWORD, GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET, MAIL_HOST, MAIL_USERNAME, MAIL_PASSWORD and MAIL_FROM. Configure database TLS per your provider. A STARTTLS SMTP service is assumed. WEB_URL must match the HTTPS origin and the Google callback is WEB_URL/login/oauth2/code/google.

Create matching TLS Secrets and an ingress controller before enabling ingress. For private GHCR packages, create a read-only image-pull Secret and set imagePullSecrets to a list containing `name: ghcr-pull`. Never reuse a CI write token for image pulling. Namespaces must already exist: deployment credentials should be scoped to their namespace, not cluster-admin.

The frontend image is built once with BACKEND_URL=http://little-list-backend:8080. Each namespace has that internal service name, so the identical images can be promoted between environments. Only Next is exposed by ingress; it proxies API, short-link and OAuth paths to Spring. These images are specifically configured for this Kubernetes service layout. For Render or another topology, rebuild with its backend address; BACKEND_URL is baked at Next build time.

## Enable GitHub deployment

1. Configure GitHub Environments dev, staging, production. Set PUBLIC_HOST and KUBE_CONFIG separately. Use a narrowly scoped deploy identity; prefer short-lived cloud OIDC credentials when a cloud provider is selected.
2. Restrict deployments to main. Add required reviewers to production and disallow self-approval where available. This policy must be configured in GitHub settings; workflow YAML alone cannot enforce an approval reviewer.
3. Provision namespace, PostgreSQL, SMTP, OAuth, TLS and runtime/pull Secrets. Replace placeholder hostnames for manual Helm usage. The GitHub workflow overrides hostname from the Environment variable.
4. Require the Verify Little List checks in main-branch protection. Run the first workflow and inspect its results before relying on it as a gate.
5. From Actions → Deploy Kubernetes, select a successful full commit SHA and environment. Validate staging login, image upload, sharing/revocation, real confirmation email and purchase flow before production promotion.

Manual equivalent, after selecting the intended kubeconfig/context and supplying a real image tag:

```sh
helm upgrade --install little-list deploy/helm/little-list \
  --namespace little-list-staging \
  -f deploy/environments/staging.yaml \
  --set-string image.tag=FULL_TESTED_COMMIT_SHA \
  --set-string ingress.host=staging.your-domain.in \
  --set-string config.WEB_URL=https://staging.your-domain.in \
  --atomic --wait --timeout 10m
```

## Kubernetes limits

The chart provides two Deployments, internal Services, backend ConfigMap/Secret references, probes, resource requests/limits, non-root security context and optional TLS ingress. It does not provision a database, certificate issuer, SMTP service, secret manager or network policy. Limit pod network access using your cluster's chosen CNI and ingress-controller labels before public production.

One backend replica is intentional because sessions are currently in memory. Recreate rollouts cause brief downtime and log users out. Add shared Spring Session storage and validate multi-instance behavior before horizontal scaling/rolling updates. Flyway runs at backend startup. Helm atomic rollback cannot undo database migrations; migrations must be backward-compatible and backups/restores tested. Rotating a runtime Secret requires a backend rollout; ConfigMap changes trigger one automatically. The chart has been linted/rendered locally, not installed into a live cluster.

## Jenkins alternative

Jenkinsfile supplies the same core CI checks and a real container/SMTP smoke. Configure a Multibranch Pipeline from this GitHub repository with script path Jenkinsfile. Agent label little-list-linux requires Java21, Node24, Docker Engine + Compose, Git and Helm3.17.3. Install Pipeline, Git and JUnit plugins. Use an isolated trusted agent with exclusive access to ports3000/5432/1025/8025; do not run this job on the developer's machine. Per-job concurrency is disabled; separate branch jobs still need separate agents. Never allow untrusted fork code on agents with deployment credentials or a privileged Docker socket.

GitHub Actions is the default publisher/deployer. Jenkins is an alternative CI runner, not a second automatic production deployer. If you choose Jenkins as the release owner later, disable GitHub publishing/deployment and add Jenkins-scoped registry credentials, environment-specific kubeconfig file credentials and an approval stage. A Jenkins server/webhook is not configured by committing a Jenkinsfile. No Jenkins execution has been claimed.

Official references: [GitHub container publishing](https://docs.github.com/en/actions/tutorials/publish-packages/publish-docker-images), [Jenkins Pipeline syntax](https://www.jenkins.io/doc/book/pipeline/syntax/), [Jenkins credentials](https://www.jenkins.io/doc/book/using/using-credentials/), [Helm lint](https://helm.sh/docs/helm/helm_lint/).
