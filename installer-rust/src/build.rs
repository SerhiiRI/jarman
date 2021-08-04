use winres;


#[cfg(windows)]
fn main() {
    let mut res = winres::WindowsResource::new();
    res.set_icon("resources/setup.ico")
        .set("Setup", "Setup.exe")
        // manually set version 1.0.0.0
        .set_version_info(winres::VersionInfo::PRODUCTVERSION, 0x0001000000000000);
    res.compile().unwrap();
}

#[cfg(unix)]
fn main() {
}