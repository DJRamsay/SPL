# Compile
Write-Host "Compiling Java files..." -ForegroundColor Cyan
$files = Get-ChildItem -Path .\src\main\java -Recurse -Filter *.java | ForEach-Object { $_.FullName }
javac -d out $files

# Check if compilation succeeded
if ($LASTEXITCODE -eq 0) {
    Write-Host "Compilation successful!" -ForegroundColor Green
    Write-Host "Running program..." -ForegroundColor Cyan
    java -cp out Main
} else {
    Write-Host "Compilation failed!" -ForegroundColor Red
    exit 1
}