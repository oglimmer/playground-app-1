{{/* Base name */}}
{{- define "wiki.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/* Fully-qualified release name */}}
{{- define "wiki.fullname" -}}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := default .Chart.Name .Values.nameOverride -}}
{{- if contains $name .Release.Name -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}
{{- end -}}

{{- define "wiki.backend.fullname" -}}
{{- printf "%s-backend" (include "wiki.fullname" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "wiki.frontend.fullname" -}}
{{- printf "%s-frontend" (include "wiki.fullname" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "wiki.postgres.fullname" -}}
{{- printf "%s-postgres" (include "wiki.fullname" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/* External secret name */}}
{{- define "wiki.secretName" -}}
{{- if .Values.existingSecret -}}
{{- .Values.existingSecret -}}
{{- else -}}
{{- printf "%s-secret" (include "wiki.fullname" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}

{{- define "wiki.serviceAccountName" -}}
{{- if .Values.serviceAccount.create -}}
{{- default (include "wiki.fullname" .) .Values.serviceAccount.name -}}
{{- else -}}
{{- default "default" .Values.serviceAccount.name -}}
{{- end -}}
{{- end -}}

{{/* Common labels */}}
{{- define "wiki.labels" -}}
helm.sh/chart: {{ printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
app.kubernetes.io/name: {{ include "wiki.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}

{{/* Selector labels for a given component (pass a dict with "root" and "component") */}}
{{- define "wiki.selectorLabels" -}}
app.kubernetes.io/name: {{ include "wiki.name" .root }}
app.kubernetes.io/instance: {{ .root.Release.Name }}
app.kubernetes.io/component: {{ .component }}
{{- end -}}

{{/* Backend JDBC URL — bundled Postgres service or external DB */}}
{{- define "wiki.dbUrl" -}}
{{- if .Values.postgres.enabled -}}
{{- printf "jdbc:postgresql://%s:5432/%s" (include "wiki.postgres.fullname" .) .Values.postgres.database -}}
{{- else -}}
{{- .Values.externalDatabase.url -}}
{{- end -}}
{{- end -}}

{{- define "wiki.dbUsername" -}}
{{- if .Values.postgres.enabled -}}
{{- .Values.postgres.username -}}
{{- else -}}
{{- .Values.externalDatabase.username -}}
{{- end -}}
{{- end -}}

{{/* Image ref: repository:tag where tag defaults to chart appVersion */}}
{{- define "wiki.image" -}}
{{- $tag := .image.tag | default .root.Chart.AppVersion -}}
{{- printf "%s:%s" .image.repository $tag -}}
{{- end -}}
