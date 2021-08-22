
$TMPPackageFolder = 'transact-package'
$PackageName = 'jarman-1.1.1-windows.zip'
New-Item -Path "." -Name "$TMPPackageFolder" -ItemType "directory"
Copy-Item -Path '.jarman.d\plugins' -Recurse -DestinationPath "$TMPPackageFolder\plugins"
Copy-Item -Path '.jarman.d\configs' -Recurse -DestinationPath "$TMPPackageFolder\configs"
Copy-Item -Path '.jarman' -Destination "$TMPPackageFolder"
Copy-Item -Path '.jarman.data.clj' -Destination "$TMPPackageFolder"
# Copy-Item -Path 'Jarman EXE' -Destination "$TMPPackageFolder"
Compress-Archive -Path "$TMPPackageFolder" -DestinationPath "$PackageName"

$File = "D:\Dev\somefilename.zip";
$ftp = "ftp://jarman:dupa@trashpanda-team.ddns.net/jarman/$PackageName"

Write-Host -Object "ftp url: $ftp";

$webclient = New-Object -TypeName System.Net.WebClient;
$uri = New-Object -TypeName System.Uri -ArgumentList $ftp;

Write-Host -Object "Uploading $File...";

$webclient.UploadFile($uri, $File);

