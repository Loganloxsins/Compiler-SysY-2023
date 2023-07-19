; ModuleID = 'module'
source_filename = "module"

@"globalArrayVar:c" = global <2 x i32> <i32 1, i32 2>

define i32 @func() {
"FunctionBlock: funcEntry":
}

define i32 @main() {
"FunctionBlock: mainEntry":
  %"array: a" = alloca <2 x i32>, align 8
  %arrayInitPointer = getelementptr <2 x i32>, <2 x i32>* %"array: a", i32 0, i32 0
  store i32 1, i32* %arrayInitPointer, align 4
  %arrayInitPointer1 = getelementptr <2 x i32>, <2 x i32>* %"array: a", i32 0, i32 1
  store i32 5, i32* %arrayInitPointer1, align 4
  %"array: b" = alloca <3 x i32>, align 16
  %arrayInitPointer2 = getelementptr <3 x i32>, <3 x i32>* %"array: b", i32 0, i32 0
  store i32 1, i32* %arrayInitPointer2, align 4
  %arrayInitPointer3 = getelementptr <3 x i32>, <3 x i32>* %"array: b", i32 0, i32 1
  store i32 4, i32* %arrayInitPointer3, align 4
  %arrayInitPointer4 = getelementptr <3 x i32>, <3 x i32>* %"array: b", i32 0, i32 2
  store i32 14, i32* %arrayInitPointer4, align 4
  %getArrayValuePointer = getelementptr <2 x i32>, <2 x i32>* %"array: a", i32 0
  %getArrayValuePointer5 = getelementptr <3 x i32>, <3 x i32>* %"array: b", i32 0
  %addRes = add <2 x i32>* %"array: a", <3 x i32>* %"array: b"
  ret <2 x i32>* %addRes
  %"var: a" = alloca i32, align 4
  store i32 10, i32* %"var: a", align 4
}

define i32 @main.1() {
"FunctionBlock: mainEntry":
  %"var: b" = alloca i32, align 4
  %"var: a" = load i32, i32* %"var: a", align 4
  %"cond: ==" = icmp eq i32 %"var: a", 100
  %"to i32" = zext i1 %"cond: ==" to i32
  %"not zero" = icmp ne i32 %"to i32", 0
  br i1 %"not zero", label %"if true: ", label %"if false: "

"if true: ":                                      ; preds = %"FunctionBlock: mainEntry"
  %"var: b1" = load i32, i32* %"var: b", align 4
  store i32 99999, i32 %"var: b1", align 4
  br label %"if complete"

"if false: ":                                     ; preds = %"FunctionBlock: mainEntry"
  %"var: a2" = load i32, i32* %"var: a", align 4
  store i32 666666, i32 %"var: a2", align 4
  br label %"if complete"

"if complete":                                    ; preds = %"if false: ", %"if true: "
  %"var: a3" = load i32, i32* %"var: a", align 4
  ret i32 %"var: a3"
}
