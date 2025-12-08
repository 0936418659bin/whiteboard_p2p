# Compile P2P Whiteboard without Maven
# PowerShell Script để biên dịch all Java files

$sourceDir = "$PSScriptRoot\src\main\java"
$targetDir = "$PSScriptRoot\target\classes"
$libDir = "$PSScriptRoot\lib"

# Tạo target directory nếu chưa có
if (-Not (Test-Path $targetDir)) {
    New-Item -ItemType Directory -Path $targetDir | Out-Null
    Write-Host "✓ Created $targetDir"
}

# Kiểm tra Java compiler
$javac = "javac"
Write-Host "Checking Java compiler..."
& $javac -version 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Error: javac not found!"
    exit 1
}
Write-Host "✓ Java compiler found"

# Find all *.jar files in lib directory
$classpath = @()
if (Test-Path $libDir) {
    $jars = Get-ChildItem -Path $libDir -Filter "*.jar"
    if ($jars) {
        foreach ($jar in $jars) {
            $classpath += $jar.FullName
        }
        Write-Host "✓ Found $($jars.Count) JAR files in lib/"
    }
}

# Build classpath string
$classpathStr = if ($classpath.Count -gt 0) { $classpath -join ";" } else { $libDir }

Write-Host ""
Write-Host "Starting compilation..."
Write-Host "Source: $sourceDir"
Write-Host "Output: $targetDir"
Write-Host "Classpath: $classpathStr"
Write-Host ""

# Compile all Java files
$javaFiles = @()
Get-ChildItem -Path $sourceDir -Filter "*.java" -Recurse | ForEach-Object {
    $javaFiles += $_.FullName
}

if ($javaFiles.Count -eq 0) {
    Write-Host "❌ No Java files found!"
    exit 1
}

Write-Host "Found $($javaFiles.Count) Java files to compile"
Write-Host ""

# Compile
& $javac -d $targetDir -cp "$classpathStr" -encoding UTF-8 $javaFiles

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "✓ Compilation successful!"
    Write-Host "Output directory: $targetDir"
    
    # Count compiled classes
    $classFiles = Get-ChildItem -Path $targetDir -Filter "*.class" -Recurse
    Write-Host "✓ Generated $($classFiles.Count) class files"
    exit 0
} else {
    Write-Host ""
    Write-Host "❌ Compilation failed!"
    exit 1
}
