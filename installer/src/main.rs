mod tools;

use std::fs;
use std::path;
use regex;
use tools::unziper::{unzip};
use tools::files::{copy_dir, install_path_resolver};
use std::env as environment;
use ftp::FtpStream;
use std::io::{BufWriter, Write, Read, ErrorKind};
use std::fs::remove_file;
use std::borrow::Borrow;
use regex::{Regex, Error};

const PROGRAM:&str = "Jarman";
const FTP_LOGIN:&str = "jarman";
const FTP_PASSWORD:&str = "dupa";

fn ftp_file_list() -> Vec<String> {
    let mut file_list = Vec::new();
    let mut ftp_stream = FtpStream::connect("trashpanda-team.ddns.net:21").unwrap();
    &ftp_stream.login(FTP_LOGIN, FTP_PASSWORD).unwrap();
    println!("PWD: {}", &ftp_stream.pwd().unwrap());
    for ftp_file in &ftp_stream.nlst(Some(".")).unwrap(){
        println!("\'{}\'",ftp_file);
        file_list.push(ftp_file.to_string());
    }
    file_list

    // ftp_stream.retr("jarman.txt",|stream|{
    //     let mut buffer = Vec::new();
    //     stream.read_to_end(&mut buffer).unwrap();
    //     let mut f = fs::File::open("jarman.txt").unwrap();
    //     f.write(buffer.as_slice()).unwrap();
    //     Ok(true)
    // });

    // let mut buff = Vec::new();
    // let mut buffer = ftp_stream.get("jarman.txt").unwrap();
    // let mut f = fs::File::create("jarman.txt").unwrap();
    // buffer.read_to_end(&mut buff);
    // f.write(buff.as_slice()).unwrap();

    // let f = fs::File::open("jarman.txt").unwrap();
    // let f = BufWriter::new(f);
    // s.re
}
#[derive(Debug,Clone)]
pub struct PandaPackage{
    pub file: Option<String>,
    pub name: Option<String>,
    pub version: Option<String>,
    pub artifacts: Option<String>,
    pub uri: Option<String>
}
impl PandaPackage {
    fn re_panda_pattern() -> Result<Regex, Error> {regex::Regex::new(r"(\w+)-(\w+\.\w+\.\w+)[-.]{0,1}(.+)")}
    fn create(file: &str, name: &str, version: &str, artifacts: &str, uri: &str) -> Option<PandaPackage> {
        Some(PandaPackage {file: Some(file.to_string())
            ,name: Some(file.to_string())
            ,version: Some(file.to_string())
            ,artifacts: Some(file.to_string())
            ,uri: Some(file.to_string())
        })
    }
    fn create_from_file(file: &str, uri: &str) -> Option<PandaPackage> {
        let get_field = |m: Option<regex::Match>| -> Option<String> {
            if let Some(s) = m {
                return Some(s.as_str().to_string());
            } else { return None };
        };
        let pattern = PandaPackage::re_panda_pattern().unwrap();
        for cap in pattern.captures_iter(file) {
            return Some(PandaPackage {
                file: get_field(cap.get(0)),
                name: get_field(cap.get(1)),
                version: get_field(cap.get(2)),
                artifacts: get_field(cap.get(3)),
                uri: Some(uri.to_string())
            });
        }
        return None
    }
}

fn ftp_get_all_packages(repositories:&[&str]) -> Vec<PandaPackage>{
    let mut packagelist:Vec<PandaPackage> = Vec::new();
    for repo_url in repositories {
        if let Ok(mut ftp_stream) = FtpStream::connect(repo_url) {
            if let Ok(_s) =  &ftp_stream.login("jarman", "dupa") {
                for ftp_file in &ftp_stream.nlst(Some(".")).unwrap() {
                    if let Some(package) = PandaPackage::create_from_file(ftp_file, repo_url){
                        packagelist.push(package);
                    }
                }
            }
        }
    }
    packagelist
}

fn ftp_download_package(package: PandaPackage) -> Result<(), String>{
    match package {
        PandaPackage{file:Some(file_name), name, version, artifacts, uri:Some(url)} => {
            if let mut ftp_stream = FtpStream::connect(url).unwrap() {
                if let Ok(_some) =  &ftp_stream.login("jarman", "dupa") {
                    let mut buff = Vec::new();
                    let mut buffer = ftp_stream.get(&file_name.as_str()).unwrap();
                    let mut f = fs::File::create(&file_name.as_str()).unwrap();
                    buffer.read_to_end(&mut buff);
                    f.write(buff.as_slice()).unwrap();
                    return Ok(());
                }
            }
        }
        _ => {println!("Package is corrupted {:?}", package)}
    }
    Err("Other type error".to_string())
}

fn bigger<'a>(pkg1:&'a PandaPackage,pkg2:&'a PandaPackage) -> &'a PandaPackage{
    let to_u32 = |m:Option<regex::Match>|->u32{m.unwrap().as_str().parse().unwrap()};
    let p1 = &pkg1.version;
    let p2 = &pkg2.version;
    if let (Some(version1), Some(version2)) = (p1, p2) {
        let r = regex::Regex::new(r"(\d+).(\d+).(\d+)").unwrap();
        let c1 = r.captures_iter(version1.as_str());
        let c2 = r.captures_iter(version2.as_str());
        for (s1, s2) in c1.zip(c2){
            let (_p1v1,_p2v1) = (to_u32(s1.get(1)),to_u32(s2.get(1)));
            let (_p1v2,_p2v2) = (to_u32(s1.get(2)),to_u32(s2.get(2)));
            let (_p1v3,_p2v3) = (to_u32(s1.get(3)),to_u32(s2.get(3)));
            if _p1v1 != _p2v1{
                if _p1v1 > _p2v1 { return &pkg1 } else { return &pkg2 }
            } else if _p1v2 != _p2v2{
                if _p1v2 > _p2v2 { return &pkg1 } else { return &pkg2 }
            } else if _p1v3 != _p2v3{
                if _p1v3 > _p2v3 { return &pkg1 } else { return &pkg2 }
            } else { return &pkg1 }
        }
    }
    return &pkg1;
}

fn version_resolver(packages:&[PandaPackage]) -> Option<PandaPackage> {
    match packages.len() {
        0 => return None,
        1 => return Some(packages[0].clone()),
        _ => return Some(packages.iter().fold(packages[0].to_owned(), |acc:PandaPackage, pkg:&PandaPackage|bigger(&acc,pkg).to_owned()))
    }
}


// fn is_package_allowed(file_name:&str) {
//     let patternnn = regex::Regex::new(r"(\w+)-(\w+\.\w+\.\w+)[-.]{0,1}(.+)").unwrap();
//     println!("All matche");
//     for cap in pattern.captures_iter(file_name){
//         println!("{:?}",cap)
//     }
// }


fn main() {
    // unzip("/home/serhii/Downloads/zip_5MB.zip");
    // let installation_folder = install_path_resolver();
    // environment::set_current_dir(&installation_folder.as_str());
    // unzip("/home/serhii/Downloads/zip_5MB.zip");
    // println!("Installed to {}", &installation_folder.as_str());
    // copy_dir(&path::Path::new("./Jarman/zip_10MB"), &path::Path::new("./Jarman"))
    //ftp_file_list()
    // is_package_allowed("jarman-1.0.13.zip");

    // let all_packages = ftp_get_all_packages(&["trashpanda-team.ddns.net:21"]);
    // println!("packages count {} ", all_packages.len())

    // let mut test_packages:Vec<PandaPackage> = Vec::new();
    // test_packages.push(PandaPackage::create_from_file("jarman-0.1.1.zip", "<some urls>").unwrap());
    // test_packages.push(PandaPackage::create_from_file("jarman-0.1.4.zip", "<some urls>").unwrap());
    // test_packages.push(PandaPackage::create_from_file("jarman-1.0.14.zip", "<some urls>").unwrap());
    // test_packages.push(PandaPackage::create_from_file("jarman-0.1.0.zip", "<some urls>").unwrap());
    // println!("[Selected] -> {:?}", version_resolver(test_packages.as_slice()).unwrap())
}
















