; ModuleID = 'module'
source_filename = "module"

@g_var = global i32 2

define i32 @main() {
mainEntry:
  %a = alloca i32, align 4
  %g_var = load i32, i32* @g_var, align 4
  store i32 %g_var, i32* %a, align 4
  %g_var1 = load i32, i32* @g_var, align 4
  %a2 = load i32, i32* %a, align 4
  ret i32 %a2
}
