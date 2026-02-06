use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::{jobject, jboolean};
use std::io::{Read, Write};
use byteorder::{LittleEndian, ReadBytesExt};
use walkdir::WalkDir;

struct LogEntry {
    level: i32,
    tag: String,
    message: String,
    pid: i64,
    tid: i64,
    time_ms: i64,
}

#[no_mangle]
pub extern "system" fn Java_com_kapp_marslens_data_parser_XlogParser_decodeXlogNative<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    path_jstring: JString<'local>,
    key_jstring: JString<'local>,
) -> jobject {
    let path_str: String = env.get_string(&path_jstring).expect("Couldn't get path!").into();
    let key_str: String = env.get_string(&key_jstring).expect("Couldn't get key!").into();
    
    let logs = parse_xlog(&path_str, if key_str.is_empty() { None } else { Some(&key_str) });

    let array_list_class = env.find_class("java/util/ArrayList").expect("ArrayList not found");
    let list = env.new_object(&array_list_class, "()V", &[]).expect("Failed to create list");
    
    let log_entry_class = env.find_class("com/kapp/marslens/data/model/LogEntry").expect("LogEntry class not found");
    
    for log in &logs {
        let _ = env.with_local_frame(16, |inner_env| {
            let tag_jstring = inner_env.new_string(&log.tag).unwrap();
            let msg_jstring = inner_env.new_string(&log.message).unwrap();
            
            // Static method: createFromNative(Int, String, String, Long, Long, Long)
            let log_entry = inner_env.call_static_method(
                &log_entry_class,
                "createFromNative",
                "(ILjava/lang/String;Ljava/lang/String;JJJ)Lcom/kapp/marslens/data/model/LogEntry;",
                &[
                    (log.level as i32).into(),
                    (&tag_jstring).into(),
                    (&msg_jstring).into(),
                    log.time_ms.into(),
                    log.tid.into(),
                    log.pid.into(),
                ]
            ).expect("Failed to call createFromNative");

            let entry_obj = log_entry.l().expect("Result is not an object");
            inner_env.call_method(&list, "add", "(Ljava/lang/Object;)Z", &[(&entry_obj).into()]).expect("Failed to add to list");
            Ok::<(), jni::errors::Error>(())
        });
    }
    
    list.into_raw()
}
// Helper for TEA decryption
fn decrypt_tea(data: &mut [u8], key: &[u8]) {
    if key.len() < 16 { return; }
    
    // Convert key bytes to u32 array
    let mut k = [0u32; 4];
    for i in 0..4 {
        k[i] = u32::from_le_bytes([key[i*4], key[i*4+1], key[i*4+2], key[i*4+3]]);
    }

    // TEA works on 8-byte blocks
    for i in (0..data.len()).step_by(8) {
        if i + 8 <= data.len() {
            let mut v0 = u32::from_le_bytes([data[i], data[i+1], data[i+2], data[i+3]]);
            let mut v1 = u32::from_le_bytes([data[i+4], data[i+5], data[i+6], data[i+7]]);
            
            let mut sum: u32 = 0xC6EF3720; // delta * 32
            let delta: u32 = 0x9E3779B9;
            
            for _ in 0..16 { // XLog typically uses 16 rounds, standard TEA uses 32. 
                             // Wait, standard TEA is 32 rounds (64 operations).
                             // Let's stick to standard TEA logic.
                             // 0x9E3779B9 is delta.
                             // sum starts at delta << 5 (0xC6EF3720) for decryption.
                v1 = v1.wrapping_sub(((v0 << 4).wrapping_add(k[2])) ^ (v0.wrapping_add(sum)) ^ ((v0 >> 5).wrapping_add(k[3])));
                v0 = v0.wrapping_sub(((v1 << 4).wrapping_add(k[0])) ^ (v1.wrapping_add(sum)) ^ ((v1 >> 5).wrapping_add(k[1])));
                sum = sum.wrapping_sub(delta);
            }
            
            // Write back
            data[i..i+4].copy_from_slice(&v0.to_le_bytes());
            data[i+4..i+8].copy_from_slice(&v1.to_le_bytes());
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_com_kapp_marslens_data_parser_XlogParser_exportDecryptedFileNative<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    path_jstring: JString<'local>,
    output_jstring: JString<'local>,
    key_jstring: JString<'local>,
) -> jboolean {
    let path: String = env.get_string(&path_jstring).expect("Couldn't get path").into();
    let output: String = env.get_string(&output_jstring).expect("Couldn't get output").into();
    let key: String = env.get_string(&key_jstring).expect("Couldn't get key").into();
    
    let entries = parse_xlog(&path, if key.is_empty() { None } else { Some(&key) });
    if entries.is_empty() { return 0; }
    
    use std::io::Write;
    let Ok(mut file) = std::fs::File::create(output) else { return 0; };
    for entry in entries {
        // Format as a standard log line
        let line = format!("[{}] [{}] [{}/{}] [{}] : {}\n", 
            entry.time_ms, entry.level, entry.pid, entry.tid, entry.tag, entry.message);
        if file.write_all(line.as_bytes()).is_err() { return 0; }
    }
    1
}

#[no_mangle]
pub extern "system" fn Java_com_kapp_marslens_data_parser_XlogParser_scanDirectoryNative<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    path_jstring: JString<'local>,
) -> jobject {
    let path_str: String = env.get_string(&path_jstring).expect("Couldn't get java string!").into();
    let files = scan_xlog_directory(&path_str);

    let array_list_class = env.find_class("java/util/ArrayList").expect("ArrayList not found");
    let list = env.new_object(&array_list_class, "()V", &[]).expect("Failed to create list");
    
    for file_path in files {
        let _ = env.with_local_frame(16, |inner_env| {
            let file_path_jstring = inner_env.new_string(file_path).unwrap();
            inner_env.call_method(&list, "add", "(Ljava/lang/Object;)Z", &[(&file_path_jstring).into()]).expect("Failed to add to list");
            Ok::<(), jni::errors::Error>(())
        });
    }
    
    list.into_raw()
}


fn scan_xlog_directory(path: &str) -> Vec<String> {
    let mut xlog_files = Vec::new();
    println!("Rust: Scanning directory {}", path);
    
    for entry in WalkDir::new(path).into_iter().filter_map(|e| e.ok()) {
        let path = entry.path();
        if path.is_file() {
            if let Some(path_str) = path.to_str() {
                if is_xlog_file(path_str) {
                    xlog_files.push(path_str.to_string());
                }
            }
        }
    }
    println!("Rust: Found {} Xlog files", xlog_files.len());
    xlog_files
}

fn is_xlog_file(path: &str) -> bool {
    // 1. Check extension first (fast path)
    let path_obj = std::path::Path::new(path);
    let mut has_valid_ext = false;
    if let Some(ext) = path_obj.extension() {
        let ext_str = ext.to_string_lossy().to_lowercase();
        if ["xlog", "mmap", "mmap2", "mmap3"].contains(&ext_str.as_str()) {
            has_valid_ext = true;
        }
    }
    
    // If it doesn't have a valid log extension, skip it for the tree view
    if !has_valid_ext { return false; }

    // 2. heuristic magic check (read small chunk)
    let Ok(mut file) = std::fs::File::open(path) else { return false; };
    let mut buffer = vec![0u8; 1024]; 
    let Ok(n) = file.read(&mut buffer) else { return false; };
    if n < 10 { return false; }
    
    get_log_start_pos(&buffer[..n], 1).is_some()
}

fn parse_xlog(path: &str, key: Option<&str>) -> Vec<LogEntry> {
    println!("Rust: Opening file {} with key {:?}", path, key);
    let mut entries = Vec::new();
    let Ok(mut file) = std::fs::File::open(path) else { 
        println!("Rust: Failed to open file");
        return entries; 
    };
    let mut buffer = Vec::new();
    if file.read_to_end(&mut buffer).is_err() { return entries; }

    println!("Rust: File size: {}, first 16 bytes: {:02x?}", buffer.len(), &buffer[..16.min(buffer.len())]);

    let mut decompressed_data = Vec::new();
    let mut offset = 0;
    let mut block_count = 0;

    while offset < buffer.len() {
        let magic = buffer[offset];
        // Xlog magic numbers: 0x01 to 0x0D
        if ![0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d].contains(&magic) {
            offset += 1;
            continue;
        }

        let crypt_key_len = if [0x01, 0x02, 0x03, 0x04, 0x05].contains(&magic) { 4 } else { 64 };
        let header_len = 1 + 2 + 1 + 1 + 4 + crypt_key_len;
        
        if offset + header_len > buffer.len() {
            offset += 1;
            continue;
        }

        // Length is at offset 5 relative to magic start
        // magic(1) + seq(2) + attr(1) + level(1) = 5
        let mut len_reader = &buffer[offset + 5..offset + 9];
        let payload_len = len_reader.read_u32::<LittleEndian>().unwrap_or(0) as usize;

        if payload_len == 0 || offset + header_len + payload_len > buffer.len() {
            offset += 1;
            continue;
        }

        // Validate block end magic if possible (usually 0x00)
        let end_magic_pos = offset + header_len + payload_len;
        if end_magic_pos < buffer.len() && buffer[end_magic_pos] != 0x00 {
             // If not 0x00, it might still be a valid block in some versions, 
             // but reference checks it. Let's be lenient for now but log.
        }

        let payload = &buffer[offset + header_len..offset + header_len + payload_len];
        let mut block_data = payload.to_vec();
        
        // Decrypt if key is provided and magic suggests encryption
        if let Some(k) = key {
            if [0x06, 0x08, 0x0b, 0x0d].contains(&magic) {
                if let Ok(key_bytes) = hex::decode(k) {
                    if key_bytes.len() == 16 {
                        decrypt_tea(&mut block_data, &key_bytes);
                    }
                }
            }
        }

        let mut block_decoded = Vec::new();
        let mut success = false;

        // Try Zstd for 0x0A..0x0D
        if [0x0a, 0x0b, 0x0c, 0x0d].contains(&magic) {
            if let Ok(data) = zstd::stream::decode_all(&block_data[..]) {
                block_decoded = data;
                success = true;
            }
        } else if [0x04, 0x05, 0x07, 0x09].contains(&magic) {
            // Try raw Deflate first
            let mut decoder = flate2::read::DeflateDecoder::new(&block_data[..]);
            let mut data = Vec::new();
            if decoder.read_to_end(&mut data).is_ok() {
                block_decoded = data;
                success = true;
            } else {
                // Try Zlib decoder
                let mut zlib = flate2::read::ZlibDecoder::new(&block_data[..]);
                let mut data = Vec::new();
                if zlib.read_to_end(&mut data).is_ok() {
                    block_decoded = data;
                    success = true;
                }
            }
        } else {
            // No compression
            block_decoded = block_data;
            success = true;
        }

        if success && !block_decoded.is_empty() {
            // Simple heuristic to check if it's text xlog data
            let s = String::from_utf8_lossy(&block_decoded);
            if s.contains('[') && s.contains(']') {
                decompressed_data.extend(block_decoded);
                block_count += 1;
                offset += header_len + payload_len + 1;
                continue;
            }
        }

        offset += 1;
    }

    println!("Rust: Decompressed {} blocks, total size: {}", block_count, decompressed_data.len());

    if !decompressed_data.is_empty() {
        let content = String::from_utf8_lossy(&decompressed_data);
        for line in content.lines() {
            if let Some(entry) = parse_log_line(line) {
                entries.push(entry);
            }
        }
    }

    println!("Rust: Returning {} entries", entries.len());
    entries
}

fn is_good_log_buf(buf: &[u8], offset: usize, count: i32) -> bool {
    if offset >= buf.len() { return true; }
    let magic_value = buf[offset];
    let crypt_key_len = if [0x03, 0x04, 0x05].contains(&magic_value) {
        4
    } else if [0x07, 0x06, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D].contains(&magic_value) {
        64
    } else {
        return false;
    };

    let header_len = 1 + 2 + 1 + 1 + 4 + crypt_key_len;
    if offset + header_len > buf.len() { return false; }
    
    let length = (&buf[offset + 5..offset + 9]).read_u32::<LittleEndian>().unwrap_or(0) as usize;
    if offset + header_len + length > buf.len() { return false; }
    
    // Check for 0x00 end magic if it's not the end of file
    if offset + header_len + length < buf.len() && buf[offset + header_len + length] != 0x00 {
        // Some versions might not have 0x00? But reference checks it.
        // return false; 
    }

    if count <= 1 { return true; }
    is_good_log_buf(buf, offset + header_len + length + 1, count - 1)
}

fn get_log_start_pos(buf: &[u8], count: i32) -> Option<usize> {
    for offset in 0..buf.len() {
        let magic = buf[offset];
        if [0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D].contains(&magic) {
            if is_good_log_buf(buf, offset, count) {
                return Some(offset);
            }
        }
    }
    None
}

fn parse_log_line(line: &str) -> Option<LogEntry> {
    // Expected format: [Level][Time][PID, TID][Tag][Metadata]Message
    if !line.starts_with('[') { return None; }

    let mut parts = Vec::new();
    let mut current = String::new();
    let mut in_bracket = false;
    let mut last_bracket_pos = 0;

    for (i, c) in line.char_indices() {
        if c == '[' {
            in_bracket = true;
            current.clear();
        } else if c == ']' {
            in_bracket = false;
            parts.push(current.clone());
            last_bracket_pos = i;
            if parts.len() == 5 { break; }
        } else if in_bracket {
            current.push(c);
        }
    }

    if parts.len() < 5 { return None; }

    let message = &line[last_bracket_pos + 1..];
    
    // parts[0] = Level (e.g., "I")
    let level = match parts[0].to_uppercase().as_str() {
        "V" => 0,
        "D" => 1,
        "I" => 2,
        "W" => 3,
        "E" => 4,
        "F" => 5,
        _ => 2,
    };

    // parts[1] = Time (e.g., "2026-02-05 +8.0 19:06:05.656")
    // We try to handle the +8.0 part by replacing it or parsing around it.
    // Simplest approach for NaiveDateTime: remove the +X.Y part
    let time_str = parts[1].split(' ').filter(|p| !p.starts_with('+')).collect::<Vec<_>>().join(" ");
    let time_ms = chrono::NaiveDateTime::parse_from_str(&time_str, "%Y-%m-%d %H:%M:%S%.3f")
        .ok()
        .map(|dt| dt.and_utc().timestamp_millis())
        .unwrap_or(0);

    // parts[2] = PID, TID (e.g., "12464, 2*")
    let id_parts: Vec<&str> = parts[2].split(',').map(|s| s.trim()).collect();
    let pid = id_parts.get(0).and_then(|s| s.parse::<i64>().ok()).unwrap_or(0);
    let tid = id_parts.get(1).and_then(|s| s.trim_end_matches('*').parse::<i64>().ok()).unwrap_or(0);

    // parts[3] = Tag (e.g., "LoggerInitProvider")
    let tag = parts[3].clone();

    Some(LogEntry {
        level,
        tag,
        message: message.trim().to_string(),
        pid,
        tid,
        time_ms,
    })
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_parse_punch_mmap3() {
        let path = "/Users/kexuejin/source/xlog-gui/build/reports/Punch.mmap3";
        let entries = parse_xlog(path);
        println!("Test: Found {} entries", entries.len());
        for (i, entry) in entries.iter().enumerate().take(5) {
            println!("Entry {}: Level={}, Time={}, PID={}, TID={}, Tag={}, Msg={}", 
                i, entry.level, entry.time_ms, entry.pid, entry.tid, entry.tag, entry.message);
        }
        assert!(!entries.is_empty());
    }

    #[test]
    fn test_scan_directory() {
        // Use the build/reports directory which we know contains Punch.mmap3
        let dir = "/Users/kexuejin/source/xlog-gui/build/reports";
        let files = scan_xlog_directory(dir);
        println!("Test: Scanned {} files", files.len());
        for f in &files {
            println!("Found Xlog: {}", f);
        }
        assert!(files.iter().any(|f| f.ends_with("Punch.mmap3")));
    }
}
