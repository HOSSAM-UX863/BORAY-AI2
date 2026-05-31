Add-Type -AssemblyName System.Drawing

$sizes = @{
    "mipmap-mdpi" = 48
    "mipmap-hdpi" = 72
    "mipmap-xhdpi" = 96
    "mipmap-xxhdpi" = 144
    "mipmap-xxxhdpi" = 192
}

$baseDir = "c:/Users/Hossam/Downloads/برعي/app/src/main/res"

foreach ($dir in $sizes.Keys) {
    $size = $sizes[$dir]
    $outputPath = "$baseDir/$dir/ic_launcher.png"
    
    $bitmap = New-Object System.Drawing.Bitmap($size, $size)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.Clear([System.Drawing.Color]::Black)
    
    $font = New-Object System.Drawing.Font('Arial', ($size / 4), [System.Drawing.FontStyle]::Bold)
    $brush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 215, 0))
    
    $stringFormat = New-Object System.Drawing.StringFormat
    $stringFormat.Alignment = [System.Drawing.StringAlignment]::Center
    $stringFormat.LineAlignment = [System.Drawing.StringAlignment]::Center
    
    $graphics.DrawString('برعي', $font, $brush, ($size / 2), ($size / 2), $stringFormat)
    
    $bitmap.Save($outputPath, [System.Drawing.Imaging.ImageFormat]::Png)
    
    $graphics.Dispose()
    $bitmap.Dispose()
    
    Write-Host "Created $outputPath"
}
