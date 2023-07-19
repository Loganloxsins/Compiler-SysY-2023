; ModuleID = 'module'
source_filename = "module"

@"globalVar: a" = global i32 10

define i32 @main() {
"FunctionBlock: mainEntry":
  %"var: b" = alloca i32, align 4
  %"var: a" = load i32, i32* @"globalVar: a", align 4
  %"cond: ==" = icmp eq i32 %"var: a", 100
  %"to i32" = zext i1 %"cond: ==" to i32
  %"not zero" = icmp ne i32 %"to i32", 0
  br i1 %"not zero", label %"if true: ", label %"if false: "

"if true: ":                                      ; preds = %"FunctionBlock: mainEntry"
  store i32 99999, i32* %"var: b", align 4
  br label %"if complete"

"if false: ":                                     ; preds = %"FunctionBlock: mainEntry"
  store i32 666666, i32* @"globalVar: a", align 4
  br label %"if complete"

"if complete":                                    ; preds = %"if false: ", %"if true: "
  %"var: a1" = load i32, i32* @"globalVar: a", align 4
  ret i32 %"var: a1"
}
