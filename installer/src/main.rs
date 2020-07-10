#![windows_subsystem = "windows"]

mod tools;
use tools::package;
use tools::files;
use tools::unziper;
use std::env as environment;
use std::fs;

// web-view
use serde;
use serde::Serialize;
use serde::Deserialize;
use serde_derive;
use serde_json;
use web_view::*;
use std::path;
use crate::tools::files::make_desktop_icon;
use std::process::exit;
use crate::tools::package::PandaPackage;
use std::borrow::Borrow;
use std::io::Write;

const VISUAL_x64: &'static [u8] = include_bytes!("..\\bin\\vc_redist.x64.exe");
const VISUAL_x86: &'static [u8] = include_bytes!("..\\bin\\vc_redist.x86.exe");
const UNINSTALLR: &'static [u8] = include_bytes!("..\\bin\\uninstaller.exe");

fn main() {
    let HTML = format!(r#"
    <!doctype html>
    <html>
        <head>
            {styles}
        </head>
        <body>
            <div id="app"><div id="menu"></div> Loading...</div>
        </body>
        <!--[if gte IE 9 | !IE ]> <!-->
		{scripts}
		<![endif]-->
    </html>
    "#, styles = inline_style(include_str!("frontend/styles.css")), scripts = inline_script(include_str!("frontend/app.js")));

    let mut wv = web_view::builder()
        .title("Setup")
        .content(Content::Html(HTML))
        .size(550,350)
        .resizable(false)
        .frameless(true)
        .debug(false)
        .user_data(Status::Start)
        .invoke_handler(|w, a| {
            println!("onclick");
            w.exit();
            // exit(1);
            Ok(())
        })
        .build()
        .unwrap();

    let handle = wv.handle();

    std::thread::spawn(move || {
        handle.dispatch(|w| {render(w, Status::Start).unwrap(); Ok(())}).unwrap();
        std::thread::sleep(std::time::Duration::from_millis(300));


        let repository_list = ["trashpanda-team.ddns.net:21"];
        handle.dispatch(move |w| {render(w, Status::Progress {value: (format!("searching packages in repo {:?}",repository_list).to_string())})});
        let all_packages:Vec<package::PandaPackage> = package::FTP::ftp_get_all_packages(&repository_list);
        let package_length = all_packages.len();
        match  package_length {
            0 => {
                handle.dispatch(|w| {render(w, Status::Error {value: "Not ethernet connection or repository is empty".to_string()})});
                handle.dispatch(|w| { render (w, Status::End)});
            }
            _ => {
                handle.dispatch(move|w| {render(w, Status::Progress {value: format!("found {} compatible package  ", package_length)})});
                let candidate:package::PandaPackage = package::version_resolver(&all_packages.as_slice()).unwrap();
                let _TMP01 = candidate.file.clone();
                handle.dispatch(move |w| {render(w, Status::Progress {value: format!("automatically select best candidate: {:?}", _TMP01.unwrap())})});

                let installation_folder = files::install_path_resolver();
                let s = installation_folder.to_string();
                handle.dispatch(move |w| { render (w, Status::Progress {value : format!("installation folder {}", s.as_str())})});
                environment::set_current_dir(&installation_folder.as_str());

                handle.dispatch(move |w| { render (w, Status::Progress {value : format!("installing Microsoft Visual C++ (2015)")})});
                if cfg!(windows) {
                    let executor_path = files::absolute_path(&installation_folder).unwrap().join("visual.exe");
                    let executor_path = executor_path.as_path();
                    files::write_static_file(if cfg!(target_pointer_width = "64"){VISUAL_x64} else {VISUAL_x86}, executor_path).unwrap();
                    files::run_exe_file(executor_path);
                    files::remove_path(executor_path);
                }

                if cfg!(windows) {
                    let executor_path = files::absolute_path(&installation_folder).unwrap().join("uninstaller.exe");
                    let executor_path = executor_path.as_path();
                    files::write_static_file(UNINSTALLR, executor_path).unwrap();
                }

                // download
                handle.dispatch(move |w| { render (w, Status::Progress {value : format!("downloading package...").to_string()})});
                package::FTP::ftp_download_package(candidate.clone()).unwrap();
                handle.dispatch(move |w| { render (w, Status::Progress {value : format!("package was downloaded").to_string()})});

                // unzip
                handle.dispatch(move |w| { render (w, Status::Progress {value : format!("extraction package").to_string()})});
                unziper::unzip(&candidate.clone().file.unwrap());
                fs::remove_file(std::path::Path::new(&candidate.file.unwrap()));

                // create desktop icon
                handle.dispatch(|w| { render (w, Status::Progress {value : format!("creating desktop shortuct")})});
                make_desktop_icon(std::path::Path::new("hrtime.exe")).unwrap();

                // finish
                handle.dispatch(|w| { render (w, Status::End)});

            }

        };

    });

    wv.run().unwrap();

}

#[derive(Debug, Serialize, Deserialize)]
#[serde(tag = "status", rename_all = "camelCase")]
pub enum Status {
    Start,
    Progress {value: String},
    Error{value: String},
    End,
}

fn render(webview: &mut WebView<Status>, value: Status) -> WVResult {
    println!("rust => {}", serde_json::to_string(&value).unwrap());
    let change_status = {
        format!("render({})", serde_json::to_string(&value).unwrap())
    };
    webview.eval(&change_status)
}

fn inline_style(s: &str) -> String {
    format!(r#"<style type="text/css">{}</style>"#, s)
}

fn inline_script(s: &str) -> String {
    format!(r#"<script type="text/javascript">{}</script>"#, s)
}


