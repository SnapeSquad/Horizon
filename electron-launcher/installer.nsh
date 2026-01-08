; NSIS Custom Install Script for Horizon Launcher

!include "MUI2.nsh"

; Installer attributes
Name "Horizon Launcher"
OutFile "HorizonLauncherSetup.exe"
InstallDir "$PROGRAMFILES\Horizon Launcher"
RequestExecutionLevel admin

; Interface settings
!define MUI_ABORTWARNING
!define MUI_ICON "${NSISDIR}\Contrib\Graphics\Icons\modern-install.ico"

; Pages
!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_LICENSE "..\..\LICENSE.txt"
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH

!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES

; Languages
!insertmacro MUI_LANGUAGE "Russian"

; Installer sections
Section "Install" SEC01
    SetOutPath "$INSTDIR"
    
    ; Add files
    File /r "*.*"
    
    ; Create desktop shortcut
    CreateShortcut "$DESKTOP\Horizon Launcher.lnk" "$INSTDIR\Horizon Launcher.exe"
    
    ; Create start menu shortcuts
    CreateDirectory "$SMPROGRAMS\Horizon Launcher"
    CreateShortcut "$SMPROGRAMS\Horizon Launcher\Horizon Launcher.lnk" "$INSTDIR\Horizon Launcher.exe"
    CreateShortcut "$SMPROGRAMS\Horizon Launcher\Uninstall.lnk" "$INSTDIR\Uninstall.exe"
    
    ; Write uninstaller
    WriteUninstaller "$INSTDIR\Uninstall.exe"
    
    ; Registry keys for Add/Remove Programs
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\HorizonLauncher" "DisplayName" "Horizon Launcher"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\HorizonLauncher" "UninstallString" "$INSTDIR\Uninstall.exe"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\HorizonLauncher" "DisplayVersion" "2.0.0"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\HorizonLauncher" "Publisher" "Horizon Team"
    WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\HorizonLauncher" "NoModify" 1
    WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\HorizonLauncher" "NoRepair" 1
SectionEnd

; Uninstaller section
Section "Uninstall"
    ; Remove files
    RMDir /r "$INSTDIR"
    
    ; Remove shortcuts
    Delete "$DESKTOP\Horizon Launcher.lnk"
    RMDir /r "$SMPROGRAMS\Horizon Launcher"
    
    ; Remove registry keys
    DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\HorizonLauncher"
SectionEnd

