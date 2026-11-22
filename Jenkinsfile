// Alternative CI runner; GitHub Actions owns publishing/deployment by default.
pipeline {
  agent { label 'little-list-linux' }
  options { timeout(time: 35, unit: 'MINUTES'); disableConcurrentBuilds() }
  stages {
    stage('Backend') { steps { dir('backend') { sh 'sh mvnw -B verify' } } }
    stage('Frontend') { steps { dir('frontend') { sh 'npm ci && npm run build && npm audit --omit=dev --audit-level=high' } } }
    stage('JavaScript and Helm') { steps {
      sh 'node --test scripts/capture.test.mjs scripts/extension.test.mjs scripts/qr.test.mjs scripts/short-link.test.mjs'
      sh 'sh scripts/check-helm.sh'
    } }
    stage('Containers and SMTP smoke') { steps {
      sh 'docker compose -p little-list-jenkins -f compose.yaml -f compose.app.yaml up --build -d --wait'
      sh 'node scripts/smoke.mjs'
    } }
  }
  post {
    always {
      junit allowEmptyResults: true, testResults: 'backend/target/*-reports/TEST-*.xml'
      sh 'docker compose -p little-list-jenkins -f compose.yaml -f compose.app.yaml down'
    }
  }
}
