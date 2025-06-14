; Script generated by the Inno Setup Script Wizard.
; SEE THE DOCUMENTATION FOR DETAILS ON CREATING INNO SETUP SCRIPT FILES!

#define MyAppName "Jarman"
#define MyAppVersion "0.0.2"
#define MyAppPublisher "Trashpanda Team"
#define MyAppURL "https://www.trashpanda-team.ddns.net"
#define MyAppExeName "Jarman.exe"

[Setup]
; NOTE: The value of AppId uniquely identifies this application. Do not use the same AppId value in installers for other applications.
; (To generate a new GUID, click Tools | Generate GUID inside the IDE.)
AppId={{11A2EC83-D895-4F7F-A787-A682B07277B5}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
;AppVerName={#MyAppName} {#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
AppSupportURL={#MyAppURL}
AppUpdatesURL={#MyAppURL}
DefaultDirName={autopf64}\{#MyAppName}
DefaultGroupName={#MyAppName}
LicenseFile=C:\programs\GitHub\jarman\LICENSE
InfoBeforeFile=C:\programs\GitHub\jarman\installer\user-info.txt
; Uncomment the following line to run in non administrative install mode (install for current user only.)
;PrivilegesRequired=lowest
OutputDir=C:\programs\GitHub\jarman\installer\inno-target
OutputBaseFilename=jarman-setup
SetupIconFile=C:\programs\GitHub\jarman\installer\tools.ico
Compression=lzma
SolidCompression=yes
WizardStyle=modern

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"
Name: "polish"; MessagesFile: "compiler:Languages\Polish.isl"
Name: "ukrainian"; MessagesFile: "compiler:Languages\Ukrainian.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

[Dirs]
Name: "{app}\log"; Permissions: users-full

[Files]
Source: "C:\programs\GitHub\jarman\jarman\{#MyAppExeName}"; DestDir: "{app}"; Flags: ignoreversion; Permissions: users-full
Source: "C:\programs\GitHub\jarman\jarman\.jarman"; DestDir: "{%HOMEPATH}"; Flags: ignoreversion onlyifdoesntexist
Source: "C:\programs\GitHub\jarman\jarman\src\jarman\managment\.jarman.data"; DestDir: "{%HOMEPATH}"; Flags: ignoreversion onlyifdoesntexist
Source: "C:\programs\GitHub\jarman\jarman\.jarman.d\*"; DestDir: "{%HOMEPATH}\.jarman.d"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "C:\programs\GitHub\jarman\jarman\icons\*"; DestDir: "{app}\icons"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "C:\programs\GitHub\jarman\jarman\resources\*"; DestDir: "{app}\resources"; Flags: ignoreversion recursesubdirs createallsubdirs

; NOTE: Don't use "Flags: ignoreversion" on any shared system files

[Icons]
Name: "{group}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"
Name: "{group}\{cm:UninstallProgram,{#MyAppName}}"; Filename: "{uninstallexe}"
Name: "{autodesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; Tasks: desktopicon

