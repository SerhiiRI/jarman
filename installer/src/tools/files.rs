use std::fs;
use std::path;
use std::env as environment;
use std::io::{BufWriter, Write, Read, ErrorKind};
use std::fs::remove_file;

const PROGRAM:&str = "Jarman";

pub fn copy_dir<U: AsRef<path::Path>, V: AsRef<path::Path>>(from: U, to: V) -> Result<(), std::io::Error> {
    let mut stack = Vec::new();
    stack.push(path::PathBuf::from(from.as_ref()));

    let output_root = path::PathBuf::from(to.as_ref());
    let input_root = path::PathBuf::from(from.as_ref()).components().count();

    while let Some(working_path) = stack.pop() {
        println!("process: {:?}", &working_path);

        // Generate a relative path
        let src: path::PathBuf = working_path.components().skip(input_root).collect();

        // Create a destination if missing
        let dest = if src.components().count() == 0 {
            output_root.clone()
        } else {
            output_root.join(&src)
        };
        if fs::metadata(&dest).is_err() {
            println!(" mkdir: {:?}", dest);
            fs::create_dir_all(&dest)?;
        }

        for entry in fs::read_dir(working_path)? {
            let entry = entry?;
            let path = entry.path();
            if path.is_dir() {
                stack.push(path);
            } else {
                match path.file_name() {
                    Some(filename) => {
                        let dest_path = dest.join(filename);
                        println!("  copy: {:?} -> {:?}", &path, &dest_path);
                        fs::copy(&path, &dest_path)?;
                    }
                    None => {
                        println!("failed: {:?}", path);
                    }
                }
            }
        }
    }
    Ok(())
}

pub fn install_path_resolver() -> String {
    // concat PROGRAM to `folder_path` and create folder;
    // Return new path
    let install_to_folder= |folder_path:&str| -> String {
        let mut folder_path = folder_path.to_string();
        if folder_path.ends_with(path::MAIN_SEPARATOR) {
            folder_path.push_str(PROGRAM)
        } else {
            folder_path.push(path::MAIN_SEPARATOR);
            folder_path.push_str(PROGRAM);
        }
        let installation_folder= folder_path.as_str();
        fs::create_dir_all(path::Path::new(installation_folder)).unwrap();
        println!("Program will be installed to {} folder",installation_folder);
        installation_folder.to_string()
    };
    // depend on architecuture, select install folder
    if cfg!(windows) {
        if let Ok(s) = environment::var("PROGRAMFILES") {
            return install_to_folder(s.as_str());
        } else {

            if path::Path::new(r"C:\Program Files").exists() {
                return install_to_folder("C:\\Program Files");
            } else {
                return install_to_folder(".");
            }
        }
    } else if cfg!(unix) {
        return install_to_folder(".");
    };
    install_to_folder(".")
}



fn absolute_path<P>(path: P) -> std::io::Result<path::PathBuf> where P: AsRef<path::Path>{
    let path = path.as_ref();
    let absolute_path = if path.is_absolute() {
        path.to_path_buf()
    } else {
        environment::current_dir()?.join(path)
    };
    Ok(absolute_path)
}

pub fn make_desktop_icon(exe_path: &path::Path) -> Result<String,String> {
    if cfg!(windows) {
        use powershell_script;
        if let Ok(p) = environment::var("USERPROFILE") {
            let p = path::Path::new(p.as_str()).join("Desktop").join("hrtime.lnk");
            let link = p.to_str().unwrap();
            let _temporary = absolute_path(&exe_path).unwrap().to_owned();
            let exe = _temporary.to_str().unwrap();
            println!("Desktop -> {}", &link);
            println!("Exe -> {}", &exe);
            let create_shortcut_ps1 = format!(
                r#"
                $SourceFileLocation="{executable}"
                $ShortcutLocation="{desctop_link}"
                $WScriptShell=New-Object -ComObject WScript.Shell
                $Shortcut=$WScriptShell.CreateShortcut($ShortcutLocation)
                $Shortcut.TargetPath=$SourceFileLocation
                $Shortcut.Save()"#, executable=&exe, desctop_link=&link);
            match powershell_script::run(&create_shortcut_ps1.as_str(), true) {
                Ok(output) => {
                    println!("{}", output.to_string());
                    return Ok("created".to_string())
                }
                Err(e) => {
                    println!("{}", e.to_string());
                    return Err("fail".to_string())
                }
            }
            return Ok("".to_string())
        } else { return Err("Path not exist".to_string()) }
    }
    Err("Desktop icon can be create only on windows-based systems".to_owned())
}