$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$envPath = Join-Path $root '.env'

if (-not (Test-Path -LiteralPath $envPath)) {
    throw "Missing $envPath. Copy .env.example to .env and set the local MySQL credentials."
}

$config = @{}
Get-Content -LiteralPath $envPath | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith('#')) {
        $separator = $line.IndexOf('=')
        if ($separator -gt 0) {
            $key = $line.Substring(0, $separator).Trim()
            $value = $line.Substring($separator + 1).Trim()
            $config[$key] = $value
        }
    }
}

foreach ($key in @('MYSQL_DATABASE', 'MYSQL_USER', 'MYSQL_PASSWORD')) {
    if (-not $config.ContainsKey($key) -or
        [string]::IsNullOrWhiteSpace($config[$key]) -or
        $config[$key].StartsWith('replace-with-')) {
        throw "Set a real local value for $key in .env before starting the backend."
    }
}

if (-not $config.ContainsKey('JWT_SECRET') -or
    $config['JWT_SECRET'] -notmatch '^[A-Fa-f0-9]{64,}$') {
    throw 'Set JWT_SECRET to at least 32 random bytes encoded as hexadecimal in .env.'
}

$mysqlHost = if ($config.ContainsKey('MYSQL_HOST') -and $config['MYSQL_HOST']) { $config['MYSQL_HOST'] } else { '127.0.0.1' }
$mysqlPort = if ($config.ContainsKey('MYSQL_PORT') -and $config['MYSQL_PORT']) { $config['MYSQL_PORT'] } else { '3306' }
$corsOrigins = if ($config.ContainsKey('CORS_ALLOWED_ORIGINS') -and $config['CORS_ALLOWED_ORIGINS']) {
    $config['CORS_ALLOWED_ORIGINS']
} else {
    'http://127.0.0.1:5174,http://localhost:5174'
}

$env:DB_URL = "jdbc:mysql://${mysqlHost}:${mysqlPort}/$($config['MYSQL_DATABASE'])?serverTimezone=UTC"
$env:DB_USERNAME = $config['MYSQL_USER']
$env:DB_PASSWORD = $config['MYSQL_PASSWORD']
$env:JWT_SECRET = $config['JWT_SECRET']
$bootstrapToken = if ($config.ContainsKey('ADMIN_BOOTSTRAP_TOKEN')) { $config['ADMIN_BOOTSTRAP_TOKEN'] } else { '' }
if ($bootstrapToken.StartsWith('replace-with-')) {
    $bootstrapToken = ''
} elseif ($bootstrapToken -and $bootstrapToken.Length -lt 32) {
    throw 'Set ADMIN_BOOTSTRAP_TOKEN to at least 32 characters or leave it empty.'
}
$env:ADMIN_BOOTSTRAP_TOKEN = $bootstrapToken
$env:SERVER_PORT = '8081'
$env:CORS_ALLOWED_ORIGINS = $corsOrigins

Set-Location $PSScriptRoot
mvn spring-boot:run
exit $LASTEXITCODE
