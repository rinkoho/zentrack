#![allow(dead_code)]

use std::sync::atomic::{AtomicBool, AtomicIsize, AtomicUsize, Ordering};
use std::sync::{Arc, Mutex};
use windows_sys::Win32::Foundation::*;
use windows_sys::Win32::UI::Controls::*;
use windows_sys::Win32::UI::WindowsAndMessaging::*;
use crate::common::to_wide;

pub struct ProgressState {
    pub current_pct: AtomicUsize,
    pub current_text: Mutex<String>,
    pub finished: AtomicBool,
    pub error: Mutex<Option<String>>,
    pub hwnd: AtomicIsize,
}

impl ProgressState {
    pub fn new() -> Self {
        Self {
            current_pct: AtomicUsize::new(0),
            current_text: Mutex::new("Iniciando extracción de componentes...".to_string()),
            finished: AtomicBool::new(false),
            error: Mutex::new(None),
            hwnd: AtomicIsize::new(0),
        }
    }
}

unsafe extern "system" fn progress_callback(
    hwnd: HWND,
    msg: u32,
    _wparam: WPARAM,
    _lparam: LPARAM,
    lprefdata: isize,
) -> windows_sys::core::HRESULT {
    if lprefdata == 0 {
        return 0;
    }
    let state = &*(lprefdata as *const ProgressState);

    if msg == TDN_CREATED as u32 {
        state.hwnd.store(hwnd, Ordering::SeqCst);
        let range = (0 & 0xFFFF) | ((100 & 0xFFFF) << 16);
        SendMessageW(hwnd, TDM_SET_PROGRESS_BAR_RANGE as u32, 0, range as isize);
        SendMessageW(hwnd, TDM_SET_PROGRESS_BAR_POS as u32, 0, 0);
    } else if msg == TDN_TIMER as u32 {
        let pct = state.current_pct.load(Ordering::Relaxed);
        SendMessageW(hwnd, TDM_SET_PROGRESS_BAR_POS as u32, pct, 0);

        if let Ok(guard) = state.current_text.try_lock() {
            let wide = to_wide(&guard);
            SendMessageW(hwnd, TDM_SET_ELEMENT_TEXT as u32, TDE_CONTENT as usize, wide.as_ptr() as isize);
        }

        if state.finished.load(Ordering::Relaxed) {
            SendMessageW(hwnd, TDM_CLICK_BUTTON as u32, IDOK as usize, 0);
        }
    }

    0
}

pub fn show_welcome_dialog() -> bool {
    let title = to_wide("Instalador Oficial de ZenTrack");
    let instruction = to_wide("Bienvenido a ZenTrack Ultra-Low Latency");
    let content = to_wide(
        "⚡ ZenTrack configurará en tu equipo:\n\n\
        • Servidor nativo de 500Hz para Trackpad háptico y Teclado mecánico\n\
        • Conexión por cable USB con latencia cero (<0.2ms) con ADB integrado\n\
        • Soporte nativo para Mando virtual de Xbox 360 (ViGEmBus)\n\
        • Accesos directos en Escritorio y Menú Inicio\n\n\
        ¿Deseas instalar ZenTrack ahora en este equipo?"
    );
    let footer = to_wide("ZenTrack v1.0.0 • Rendimiento Ultra-Low Latency para Windows");

    let btn1 = to_wide("Instalar Ahora\nInstala ZenTrack en tu carpeta de usuario y configura accesos directos.");
    let btn2 = to_wide("Cancelar\nSalir del instalador sin realizar cambios.");

    let buttons = [
        TASKDIALOG_BUTTON {
            nButtonID: 101,
            pszButtonText: btn1.as_ptr(),
        },
        TASKDIALOG_BUTTON {
            nButtonID: 102,
            pszButtonText: btn2.as_ptr(),
        },
    ];

    let mut config: TASKDIALOGCONFIG = unsafe { std::mem::zeroed() };
    config.cbSize = std::mem::size_of::<TASKDIALOGCONFIG>() as u32;
    config.dwFlags = TDF_USE_COMMAND_LINKS | TDF_POSITION_RELATIVE_TO_WINDOW;
    config.pszWindowTitle = title.as_ptr();
    config.pszMainInstruction = instruction.as_ptr();
    config.pszContent = content.as_ptr();
    config.pszFooter = footer.as_ptr();
    config.Anonymous1.pszMainIcon = TD_INFORMATION_ICON;
    config.Anonymous2.pszFooterIcon = TD_INFORMATION_ICON;
    config.cButtons = buttons.len() as u32;
    config.pButtons = buttons.as_ptr();
    config.nDefaultButton = 101;

    let mut clicked_button: i32 = 0;
    let res = unsafe {
        TaskDialogIndirect(
            &config,
            &mut clicked_button,
            std::ptr::null_mut(),
            std::ptr::null_mut(),
        )
    };

    if res == 0 {
        clicked_button == 101
    } else {
        let fallback_res = unsafe {
            MessageBoxW(
                0,
                content.as_ptr(),
                title.as_ptr(),
                MB_YESNO | MB_ICONINFORMATION | MB_DEFBUTTON1,
            )
        };
        fallback_res == IDYES
    }
}

pub fn show_progress_dialog(state: &Arc<ProgressState>) -> bool {
    let title = to_wide("Instalador Oficial de ZenTrack");
    let instruction = to_wide("Instalando ZenTrack Ultra-Low Latency...");
    let content = to_wide("Iniciando la extracción de archivos en tu equipo...");

    let mut config: TASKDIALOGCONFIG = unsafe { std::mem::zeroed() };
    config.cbSize = std::mem::size_of::<TASKDIALOGCONFIG>() as u32;
    config.dwFlags = TDF_SHOW_PROGRESS_BAR | TDF_CALLBACK_TIMER | TDF_POSITION_RELATIVE_TO_WINDOW;
    config.pszWindowTitle = title.as_ptr();
    config.pszMainInstruction = instruction.as_ptr();
    config.pszContent = content.as_ptr();
    config.Anonymous1.pszMainIcon = TD_INFORMATION_ICON;
    config.pfCallback = Some(progress_callback);
    config.lpCallbackData = (Arc::as_ptr(state)) as isize;

    let mut clicked: i32 = 0;
    let res = unsafe {
        TaskDialogIndirect(&config, &mut clicked, std::ptr::null_mut(), std::ptr::null_mut())
    };

    res == 0
}

pub fn show_vigem_prompt() -> bool {
    let title = to_wide("Controlador de Mando Xbox 360 - ZenTrack");
    let instruction = to_wide("Se requiere instalar el controlador ViGEmBus");
    let content = to_wide(
        "🎮 Driver de Mando Xbox 360 (ViGEmBus):\n\n\
        Para que Windows reconozca tu dispositivo Android como un mando oficial\n\
        de Xbox 360, es necesario instalar el driver virtual de kernel ViGEmBus.\n\n\
        A continuación, Windows solicitará confirmación de permisos de\n\
        Administrador (Control de Cuentas de Usuario - UAC) para realizar la instalación."
    );
    let footer = to_wide("Controlador oficial open-source ViGEmBus (Nefarius)");

    let btn1 = to_wide("Instalar Controlador (Recomendado)\nHabilita la emulación de Gamepad Xbox 360 en juegos de PC.");
    let btn2 = to_wide("Omitir instalación del driver\nPuedes instalarlo manualmente después desde la carpeta de ZenTrack.");

    let buttons = [
        TASKDIALOG_BUTTON {
            nButtonID: 201,
            pszButtonText: btn1.as_ptr(),
        },
        TASKDIALOG_BUTTON {
            nButtonID: 202,
            pszButtonText: btn2.as_ptr(),
        },
    ];

    let mut config: TASKDIALOGCONFIG = unsafe { std::mem::zeroed() };
    config.cbSize = std::mem::size_of::<TASKDIALOGCONFIG>() as u32;
    config.dwFlags = TDF_USE_COMMAND_LINKS | TDF_POSITION_RELATIVE_TO_WINDOW;
    config.pszWindowTitle = title.as_ptr();
    config.pszMainInstruction = instruction.as_ptr();
    config.pszContent = content.as_ptr();
    config.pszFooter = footer.as_ptr();
    config.Anonymous1.pszMainIcon = TD_SHIELD_ICON;
    config.Anonymous2.pszFooterIcon = TD_INFORMATION_ICON;
    config.cButtons = buttons.len() as u32;
    config.pButtons = buttons.as_ptr();
    config.nDefaultButton = 201;

    let mut clicked: i32 = 0;
    let res = unsafe {
        TaskDialogIndirect(&config, &mut clicked, std::ptr::null_mut(), std::ptr::null_mut())
    };

    if res == 0 {
        clicked == 201
    } else {
        let fallback = unsafe {
            MessageBoxW(
                0,
                content.as_ptr(),
                title.as_ptr(),
                MB_YESNO | MB_ICONINFORMATION | MB_DEFBUTTON1,
            )
        };
        fallback == IDYES
    }
}

pub fn show_installed_dialog() -> bool {
    let title = to_wide("Instalación Completada - ZenTrack");
    let instruction = to_wide("🎉 ¡ZenTrack se ha instalado con éxito!");
    let content = to_wide(
        "El servidor de ultra-baja latencia se ha instalado correctamente en tu equipo.\n\n\
        • Accesos directos creados en tu Escritorio y Menú Inicio.\n\
        • El servidor iniciará en segundo plano y abrirá el Centro de Conexión en tu navegador web.\n\n\
        ¡Gracias por elegir ZenTrack!"
    );

    let btn1 = to_wide("Iniciar ZenTrack Ahora\nArranca el servidor de baja latencia y abre el navegador web.");

    let buttons = [
        TASKDIALOG_BUTTON {
            nButtonID: 301,
            pszButtonText: btn1.as_ptr(),
        },
    ];

    let mut config: TASKDIALOGCONFIG = unsafe { std::mem::zeroed() };
    config.cbSize = std::mem::size_of::<TASKDIALOGCONFIG>() as u32;
    config.dwFlags = TDF_USE_COMMAND_LINKS | TDF_POSITION_RELATIVE_TO_WINDOW;
    config.pszWindowTitle = title.as_ptr();
    config.pszMainInstruction = instruction.as_ptr();
    config.pszContent = content.as_ptr();
    config.Anonymous1.pszMainIcon = TD_INFORMATION_ICON;
    config.cButtons = buttons.len() as u32;
    config.pButtons = buttons.as_ptr();
    config.nDefaultButton = 301;

    let mut clicked: i32 = 0;
    let res = unsafe {
        TaskDialogIndirect(&config, &mut clicked, std::ptr::null_mut(), std::ptr::null_mut())
    };

    if res == 0 {
        true
    } else {
        unsafe {
            MessageBoxW(0, content.as_ptr(), title.as_ptr(), MB_OK | MB_ICONINFORMATION);
        };
        true
    }
}

pub fn show_uninstall_confirm_dialog() -> bool {
    let title = to_wide("Desinstalador Oficial de ZenTrack");
    let instruction = to_wide("¿Deseas desinstalar ZenTrack de este equipo?");
    let content = to_wide(
        "Esta operación realizará las siguientes tareas:\n\n\
        • Cerrará cualquier instancia en ejecución de ZenTrack y ADB\n\
        • Eliminará los accesos directos de Escritorio y Menú Inicio\n\
        • Eliminará los archivos instalados en la carpeta del programa\n\
        • Eliminará el registro del programa en Windows (Panel de Control)\n\n\
        ¿Estás seguro de que deseas proceder?"
    );

    let btn1 = to_wide("Desinstalar ZenTrack\nEliminar permanentemente ZenTrack y sus accesos directos.");
    let btn2 = to_wide("Cancelar\nConservar ZenTrack instalado en este equipo.");

    let buttons = [
        TASKDIALOG_BUTTON {
            nButtonID: 401,
            pszButtonText: btn1.as_ptr(),
        },
        TASKDIALOG_BUTTON {
            nButtonID: 402,
            pszButtonText: btn2.as_ptr(),
        },
    ];

    let mut config: TASKDIALOGCONFIG = unsafe { std::mem::zeroed() };
    config.cbSize = std::mem::size_of::<TASKDIALOGCONFIG>() as u32;
    config.dwFlags = TDF_USE_COMMAND_LINKS | TDF_POSITION_RELATIVE_TO_WINDOW;
    config.pszWindowTitle = title.as_ptr();
    config.pszMainInstruction = instruction.as_ptr();
    config.pszContent = content.as_ptr();
    config.Anonymous1.pszMainIcon = TD_WARNING_ICON;
    config.cButtons = buttons.len() as u32;
    config.pButtons = buttons.as_ptr();
    config.nDefaultButton = 402; // Safe default is Cancel

    let mut clicked: i32 = 0;
    let res = unsafe {
        TaskDialogIndirect(&config, &mut clicked, std::ptr::null_mut(), std::ptr::null_mut())
    };

    if res == 0 {
        clicked == 401
    } else {
        let fallback = unsafe {
            MessageBoxW(
                0,
                content.as_ptr(),
                title.as_ptr(),
                MB_YESNO | MB_ICONWARNING | MB_DEFBUTTON2,
            )
        };
        fallback == IDYES
    }
}

pub fn show_uninstall_finished_dialog() {
    let title = to_wide("Desinstalación Completada - ZenTrack");
    let instruction = to_wide("ZenTrack ha sido desinstalado correctamente");
    let content = to_wide(
        "ZenTrack se ha eliminado por completo de tu equipo:\n\n\
        • Se eliminaron los archivos del programa\n\
        • Se eliminaron los accesos directos de Escritorio y Menú Inicio\n\
        • Se eliminaron las entradas del registro de Windows\n\n\
        Gracias por haber usado ZenTrack."
    );

    let mut config: TASKDIALOGCONFIG = unsafe { std::mem::zeroed() };
    config.cbSize = std::mem::size_of::<TASKDIALOGCONFIG>() as u32;
    config.dwFlags = TDF_POSITION_RELATIVE_TO_WINDOW;
    config.dwCommonButtons = TDCBF_OK_BUTTON;
    config.pszWindowTitle = title.as_ptr();
    config.pszMainInstruction = instruction.as_ptr();
    config.pszContent = content.as_ptr();
    config.Anonymous1.pszMainIcon = TD_INFORMATION_ICON;

    let mut clicked: i32 = 0;
    let res = unsafe {
        TaskDialogIndirect(&config, &mut clicked, std::ptr::null_mut(), std::ptr::null_mut())
    };

    if res != 0 {
        unsafe {
            MessageBoxW(0, content.as_ptr(), title.as_ptr(), MB_OK | MB_ICONINFORMATION);
        };
    }
}

pub fn show_error_dialog(title: &str, instruction: &str, details: &str) {
    let title_w = to_wide(title);
    let instruction_w = to_wide(instruction);
    let details_w = to_wide(details);

    let mut config: TASKDIALOGCONFIG = unsafe { std::mem::zeroed() };
    config.cbSize = std::mem::size_of::<TASKDIALOGCONFIG>() as u32;
    config.dwFlags = TDF_POSITION_RELATIVE_TO_WINDOW;
    config.dwCommonButtons = TDCBF_OK_BUTTON;
    config.pszWindowTitle = title_w.as_ptr();
    config.pszMainInstruction = instruction_w.as_ptr();
    config.pszContent = details_w.as_ptr();
    config.Anonymous1.pszMainIcon = TD_ERROR_ICON;

    let mut clicked: i32 = 0;
    let res = unsafe {
        TaskDialogIndirect(&config, &mut clicked, std::ptr::null_mut(), std::ptr::null_mut())
    };

    if res != 0 {
        unsafe {
            MessageBoxW(0, details_w.as_ptr(), title_w.as_ptr(), MB_OK | MB_ICONERROR);
        };
    }
}
