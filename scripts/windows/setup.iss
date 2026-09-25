; =====================================================================
; ZenTrack - Inno Setup 6 Official Installer Script
; Ultra-Low Latency Trackpad, Mechanical Keyboard & Xbox Gamepad Server
; =====================================================================

#define MyAppName "ZenTrack"
#define MyAppVersion "1.0.0"
#define MyAppPublisher "ZenTrack Team"
#define MyAppURL "https://github.com/carlos/zentrack"
#define MyAppExeName "ZenTrack.exe"

[Setup]
; Basic Application Details
AppId={{D9B283A4-7541-4E89-8134-FE98A947A102}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
AppSupportURL={#MyAppURL}
AppUpdatesURL={#MyAppURL}

; Installation Paths (Per-user modern standard without admin rights upfront)
DefaultDirName={localappdata}\Programs\{#MyAppName}
DefaultGroupName={#MyAppName}
DisableProgramGroupPage=yes
PrivilegesRequired=lowest
PrivilegesRequiredOverridesAllowed=dialog

; Output configuration
OutputDir=..\..\dist\windows
OutputBaseFilename=ZenTrack-Setup-Inno
SetupIconFile=..\..\tools\installer-windows\resources\icon.ico
UninstallDisplayIcon={app}\{#MyAppExeName}

; Compression & Modern Visual Style
Compression=lzma2/ultra64
SolidCompression=yes
WizardStyle=modern
WizardResizable=no
ArchitecturesInstallIn64BitMode=x64
CloseApplications=yes
RestartApplications=no

[Languages]
Name: "spanish"; MessagesFile: "compiler:Languages\Spanish.isl"
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"

[Files]
; Main application payload
Source: "..\..\dist\windows\ZenTrack-Portable\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{userprograms}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; WorkingDir: "{app}"; IconFilename: "{app}\{#MyAppExeName}"
Name: "{userdesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; WorkingDir: "{app}"; IconFilename: "{app}\{#MyAppExeName}"; Tasks: desktopicon

[Run]
Filename: "{app}\{#MyAppExeName}"; Description: "{cm:LaunchProgram,{#StringChange(MyAppName, '&', '&&')}}"; Flags: nowait postinstall skipifsilent

[UninstallDelete]
Type: filesandordirs; Name: "{app}"

[Code]
// Win32 API bindings for kernel driver verification
function CreateFile(
  lpFileName: String;
  dwDesiredAccess, dwShareMode: DWORD;
  lpSecurityAttributes: DWORD;
  dwCreationDisposition, dwFlagsAndAttributes: DWORD;
  hTemplateFile: DWORD
): THandle;
external 'CreateFileW@kernel32.dll stdcall';

function CloseHandle(hObject: THandle): BOOL;
external 'CloseHandle@kernel32.dll stdcall';

const
  GENERIC_READ = $80000000;
  GENERIC_WRITE = $40000000;
  FILE_SHARE_READ = 1;
  FILE_SHARE_WRITE = 2;
  OPEN_EXISTING = 3;
  FILE_ATTRIBUTE_NORMAL = $80;
  INVALID_HANDLE_VALUE = -1;

// Detects if the ViGEmBus driver is currently loaded in the Windows kernel
function IsViGEmBusInstalled(): Boolean;
var
  DeviceHandle: THandle;
begin
  DeviceHandle := CreateFile(
    '\\.\ViGEmBus',
    GENERIC_READ or GENERIC_WRITE,
    FILE_SHARE_READ or FILE_SHARE_WRITE,
    0,
    OPEN_EXISTING,
    FILE_ATTRIBUTE_NORMAL,
    0
  );

  if DeviceHandle <> INVALID_HANDLE_VALUE then
  begin
    CloseHandle(DeviceHandle);
    Result := True;
  end
  else
    Result := False;
end;

// Synchronous driver installation during setup
procedure CurStepChanged(CurStep: TSetupStep);
var
  DriverPath: String;
  ResultCode: Integer;
  PromptMsg: String;
begin
  if CurStep = ssPostInstall then
  begin
    if not IsViGEmBusInstalled() then
    begin
      DriverPath := ExpandConstant('{app}\drivers\ViGEmBus_Setup.exe');
      if FileExists(DriverPath) then
      begin
        PromptMsg := 
          'Controlador de Mando Xbox 360 (ViGEmBus):' + #13#10#13#10 +
          'Para que Windows reconozca tu dispositivo como un mando oficial de Xbox 360, ' +
          'es necesario instalar el controlador de kernel ViGEmBus.' + #13#10#13#10 +
          'Windows solicitará confirmación de permisos de Administrador (UAC).' + #13#10#13#10 +
          '¿Deseas instalar el controlador ahora?';

        if MsgBox(PromptMsg, mbConfirmation, MB_YESNO) = IDYES then
        begin
          // Launch ViGEmBus setup and synchronously wait for termination
          if ShellExec('runas', DriverPath, '', '', SW_SHOWNORMAL, ewWaitUntilTerminated, ResultCode) then
          begin
            Sleep(1000);
            if IsViGEmBusInstalled() then
            begin
              Log('ViGEmBus driver verified successfully.');
            end
            else
            begin
              Log('ViGEmBus driver installer finished with code: ' + IntToStr(ResultCode));
            end;
          end;
        end;
      end;
    end;
  end;
end;

// Clean process termination before uninstalling
procedure CurUninstallStepChanged(CurUninstallStep: TUninstallStep);
var
  ResultCode: Integer;
begin
  if CurUninstallStep = usUninstall then
  begin
    // Terminate any running ZenTrack or ADB processes
    Exec('taskkill.exe', '/F /IM ZenTrack.exe', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
    Exec('taskkill.exe', '/F /IM adb.exe', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
    Sleep(500);
  end;
end;
