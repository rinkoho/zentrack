use std::env;
use std::path::PathBuf;
use std::process::Command;

fn main() {
    let target = env::var("TARGET").unwrap_or_default();
    if target.contains("windows") {
        let out_dir = PathBuf::from(env::var("OUT_DIR").unwrap());
        let res_o = out_dir.join("resource.o");

        println!("cargo:rerun-if-changed=resources/resource.rc");
        println!("cargo:rerun-if-changed=resources/installer.manifest");
        println!("cargo:rerun-if-changed=resources/icon.ico");

        let windres_cmd = if Command::new("x86_64-w64-mingw32-windres").arg("--version").output().is_ok() {
            "x86_64-w64-mingw32-windres"
        } else {
            "windres"
        };

        let status = Command::new(windres_cmd)
            .current_dir("resources")
            .args(["-i", "resource.rc", "-o"])
            .arg(&res_o)
            .status()
            .expect("Failed to execute windres");

        if !status.success() {
            panic!("windres failed with exit code: {:?}", status.code());
        }

        println!("cargo:rustc-link-arg={}", res_o.display());
    }
}
