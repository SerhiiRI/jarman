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
use crate::tools::files::make_desktop_icon;


fn main() {

    // // * * *
    // // START
    // // * * *
    //
    // let all_packages:Vec<package::PandaPackage> = package::FTP::ftp_get_all_packages(&["trashpanda-team.ddns.net:21"]);
    // println!("packages count {:?} ", all_packages.len());
    //
    // // select one
    // let candidate:package::PandaPackage = package::version_resolver(&all_packages.as_slice()).unwrap();
    // println!("[Selected] -> {:?}", &candidate.file);
    // println!("Start downloading -> {:?} ", &candidate.file);
    //
    // // install folder
    // let installation_folder = files::install_path_resolver();
    // println!("Installation folder {}", &installation_folder.as_str());
    // environment::set_current_dir(&installation_folder.as_str());
    //
    // // download
    // package::FTP::ftp_download_package(candidate.clone()).unwrap();
    // println!("Package was downloaded");
    //
    // // unzip
    // unziper::unzip(&candidate.file.unwrap());
    // fs::remove_file(std::path::Path::new(&candidate.file.unwrap()));
    //
    // // create desktop icon
    // println!("{:?}", make_desktop_icon(std::path::Path::new("hrtime.exe")));
    //
    // // finish
    // println!("Finish instalation");

    // // * * *
    // // End
    // // * * *

    let HTML = format!(r#"
    <!doctype html>
    <html>
        <head>
            {styles}
        </head>
        <body>
            <div id="app"><span onClick="window.external.invoke()"><b><span style="color: red"> X </span></b>close</span> | Loading...</div>
        </body>
        <!--[if gte IE 9 | !IE ]> <!-->
		{scripts}
		<![endif]-->
    </html>
    "#, styles = inline_style(include_str!("frontend/styles.css")), scripts = inline_script(include_str!("frontend/app.js")));

    let mut wv = web_view::builder()
        .title("Setup")
        .content(Content::Html(HTML))
        .size(100,200)
        .resizable(false)
        .debug(true)
        .user_data(Status::Start)
        .invoke_handler(|w, a| {
            w.exit();
            Ok(())
        })
        .build()
        .unwrap();

    let handle = wv.handle();

    std::thread::spawn(move || {
        for i in 0..11 {
            std::thread::sleep(std::time::Duration::from_millis(500));
            handle.dispatch(move |_web_view| {
                render(_web_view, Status::Progress { value: (format!("{}", i * 10)) }).unwrap();
                Ok(())
            }).unwrap();
        };
        std::thread::sleep(std::time::Duration::from_millis(500));
        handle.dispatch(|_web_view| {
            render(_web_view, Status::End).unwrap();
            Ok(())
        }).unwrap();
    });

    wv.run().unwrap();
    println!("The end of program");
}

#[derive(Debug, Serialize, Deserialize)]
#[serde(tag = "status", rename_all = "camelCase")]
pub enum Status {
    Start,
    Progress {value: String},
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


