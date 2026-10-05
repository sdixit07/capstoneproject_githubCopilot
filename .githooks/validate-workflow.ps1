$ErrorActionPreference = 'Stop'

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path

$mcpConfigPath = Join-Path $repoRoot '.vscode\mcp.json'
if (-not (Test-Path $mcpConfigPath)) {
    throw "MCP config file not found: $mcpConfigPath"
}

$mcpConfig = Get-Content -Path $mcpConfigPath -Raw | ConvertFrom-Json
$requiredServers = @('github-mcp', 'jira-mcp')
$missingServers = @()

foreach ($serverName in $requiredServers) {
    $server = $mcpConfig.servers.$serverName
    if (-not $server -or $server.enabled -ne $true) {
        $missingServers += $serverName
    }
}

if ($missingServers.Count -gt 0) {
    throw "MCP servers are not enabled: $($missingServers -join ', ')"
}

Write-Host 'MCP servers enabled: github-mcp, jira-mcp'

Set-Location (Join-Path $repoRoot 'ecom-project')
Write-Host 'Running backend validation...'
.\mvnw.cmd -q -Dtest=ProductControllerIT test

Set-Location (Join-Path $repoRoot 'ecom-front\ecom-catalog-react')
Write-Host 'Running frontend validation...'
npm test -- --run

Write-Host 'Workflow validation passed.'
