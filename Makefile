# Simple Makefile to help user build the rust core
all:
	cd rust_core && cargo build --release
	cp rust_core/target/release/libxlog_core.dylib composeApp/libs/ || true
	cp rust_core/target/release/xlog_core.dll composeApp/libs/ || true
	cp rust_core/target/release/libxlog_core.so composeApp/libs/ || true

clean:
	cd rust_core && cargo clean
