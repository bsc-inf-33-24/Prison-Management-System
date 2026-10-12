$ErrorActionPreference = 'Stop'
$migrationDirectory = Join-Path $PSScriptRoot '..\src\main\resources\db\migration'
$files = Get-ChildItem -LiteralPath $migrationDirectory -File -Filter 'V*__*.sql'
$versions = foreach ($migrationFile in $files) {
    if ($migrationFile.Name -notmatch '^V([0-9]+(?:_[0-9]+)*)__.+\.sql$') {
        throw "Invalid Flyway migration filename: $($migrationFile.Name)"
    }
    $parts = @($Matches[1].Split('_') | ForEach-Object { [int]$_ })
    while ($parts.Count -gt 1 -and $parts[-1] -eq 0) {
        $parts = @($parts[0..($parts.Count - 2)])
    }
    $parts -join '.'
}
$duplicates = $versions | Group-Object | Where-Object Count -gt 1
if ($duplicates) {
    throw "Duplicate Flyway versions: $($duplicates.Name -join ', ')"
}
Write-Host "Flyway migration versions are unique ($($versions.Count) files)."

