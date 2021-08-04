use std::fs;
use std::path::{Path,PathBuf};
use std::env as environment;

const PROGRAM:&str = "Jarman";
const PROGRAM_SHORTCUT:&str = "Jarman.lnk";

#[warn(irrefutable_let_patterns)]
pub fn remove_path(some_path: &Path){
    if let _ = fs::metadata(some_path)
    {
        if some_path.is_dir() {
            fs::remove_dir_all(some_path);
        } else {
            fs::remove_file(some_path);
        }
    }
}


pub fn absolute_path<P>(path: P) -> std::io::Result<PathBuf> where P: AsRef<Path>{
    let path = path.as_ref();
    let absolute_path = if path.is_absolute() {
        path.to_path_buf()
    } else {
        environment::current_dir()?.join(path)
    };
    Ok(absolute_path)
}


fn remove_installation_folder(){
    if let Ok(programfiles) = environment::var("PROGRAMFILES") {
        let directory: PathBuf = absolute_path(programfiles).unwrap();
        let directory = directory.as_path();
        let path_to_program = directory.join(PROGRAM);
        println!("{:?}", path_to_program);
        remove_path(path_to_program.as_path());
    }
}

fn remove_shortcut(){
    if let Ok(p) = environment::var("USERPROFILE") {
        let p = Path::new(p.as_str()).join("Desktop").join(PROGRAM_SHORTCUT);
        let p = p.as_path();
        remove_path(p);
    }
}


fn main(){
    remove_shortcut();
    remove_installation_folder();
}