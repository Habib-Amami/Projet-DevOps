.DEFAULT_GOAL := help

AWS_CLI ?= aws
KUBECTL ?= kubectl
AWS_REGION ?= us-east-1
EKS_CLUSTER ?= achat-eks
NAMESPACE ?= achat
GRAFANA_PORT ?= 3001
SWAGGER_PORT ?= 8090

.PHONY: help kubeconfig port-forward grafana swagger check-kubectl

help:
	@printf '%s\n' \
	  'After deployment, run these commands on your computer:' \
	  '  make kubeconfig     Connect kubectl to EKS using your current AWS credentials' \
	  '  make port-forward   Open Grafana and Swagger tunnels; Ctrl+C stops both' \
	  '  make grafana        Open only Grafana at http://127.0.0.1:$(GRAFANA_PORT)' \
	  '  make swagger        Open only Swagger at http://127.0.0.1:$(SWAGGER_PORT)/swagger-ui/index.html' \
	  '' \
	  'Overrides: AWS_REGION, EKS_CLUSTER, NAMESPACE, GRAFANA_PORT, SWAGGER_PORT' \
	  'Example: make port-forward GRAFANA_PORT=3002 SWAGGER_PORT=8091'

kubeconfig:
	$(AWS_CLI) eks update-kubeconfig --region "$(AWS_REGION)" --name "$(EKS_CLUSTER)"

check-kubectl:
	@command -v "$(KUBECTL)" >/dev/null 2>&1 || { printf '%s\n' 'kubectl is required.' >&2; exit 1; }

grafana: check-kubectl
	@printf '%s\n' 'Grafana: http://127.0.0.1:$(GRAFANA_PORT) (Ctrl+C to stop)'
	$(KUBECTL) --namespace "$(NAMESPACE)" port-forward --address=127.0.0.1 svc/grafana-lgtm "$(GRAFANA_PORT):3000"

swagger: check-kubectl
	@printf '%s\n' 'Swagger: http://127.0.0.1:$(SWAGGER_PORT)/swagger-ui/index.html (Ctrl+C to stop)'
	$(KUBECTL) --namespace "$(NAMESPACE)" port-forward --address=127.0.0.1 svc/backend "$(SWAGGER_PORT):8089"

# Keep both tunnels attached to this terminal. Stop the sibling tunnel if one
# fails, and clean up only the processes started here when this command exits.
port-forward: check-kubectl
	@set -eu; \
	grafana_pid=; swagger_pid=; \
	cleanup() { \
	  trap - EXIT INT TERM; \
	  if [ -n "$$grafana_pid" ]; then kill "$$grafana_pid" 2>/dev/null || :; fi; \
	  if [ -n "$$swagger_pid" ]; then kill "$$swagger_pid" 2>/dev/null || :; fi; \
	  wait 2>/dev/null || :; \
	}; \
	trap cleanup EXIT; \
	trap 'exit 130' INT; \
	trap 'exit 143' TERM; \
	printf '%s\n' \
	  'Grafana: http://127.0.0.1:$(GRAFANA_PORT)' \
	  'Swagger: http://127.0.0.1:$(SWAGGER_PORT)/swagger-ui/index.html' \
	  'Keep this terminal open. Press Ctrl+C to stop both tunnels.'; \
	$(KUBECTL) --namespace "$(NAMESPACE)" port-forward --address=127.0.0.1 svc/grafana-lgtm "$(GRAFANA_PORT):3000" & \
	grafana_pid=$$!; \
	$(KUBECTL) --namespace "$(NAMESPACE)" port-forward --address=127.0.0.1 svc/backend "$(SWAGGER_PORT):8089" & \
	swagger_pid=$$!; \
	while kill -0 "$$grafana_pid" 2>/dev/null && kill -0 "$$swagger_pid" 2>/dev/null; do sleep 1; done; \
	printf '%s\n' 'A port-forward stopped. Closing both tunnels; check the kubectl error above and rerun make port-forward.' >&2; \
	exit 1
