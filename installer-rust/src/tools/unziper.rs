use std::fs;
use std::io;
use std::fs::File;


fn unzip_zip_archive(archive: &mut zip::ZipArchive<File>){
    for i in 0..archive.len() {
        let mut file = archive.by_index(i).unwrap();
        let outpath = file.sanitized_name();
        {
            let comment = file.comment();
            if !comment.is_empty() {
                println!("File {} comment: {}", i, comment)
            }
        }
        // if (&*file.name()).ends_with('/'){
        if file.name().ends_with('/'){
            println!("File {} extracted to \"{}\"",i,outpath.as_path().display())
        } else {
            println!("FIle {} extracted to \"{}\" ({} bytes)", i, outpath.as_path().display(), file.size());
            if let Some(p) = outpath.parent() {
                if !p.exists() {
                    fs::create_dir_all(&p).unwrap();
                }
            }
            let mut outfile = fs::File::create(&outpath).unwrap();
            io::copy(&mut file, &mut outfile).unwrap();
        }
    }
}

pub fn unzip(file_path: &str){
    match fs::File::open(std::path::Path::new(file_path)){
        Ok(zipfile) =>
            match zip::ZipArchive::new(zipfile) {
                Ok(mut archive) => {
                    let archive  = &mut archive;
                    unzip_zip_archive(archive)},
                Err(_error) => println!("Error unpack zip file \"{}\"", file_path)
            }
        Err(_error) => println!("File with path \"{}\" not been found", file_path)
    }
}