# ZYJPdfBox

基于PdfboxV2.0.13版本修改  

-增加了对cmyk、iccBase、pattern等色彩空间支持
-移植了icc、jpeg库
-增加对cmyk编码格式的jpeg支持
-修复了clipPath精度问题
-增加了8bit色彩空间图片的支持

实现使用Android自带系统解析库与Pdfbox混合解析，解决Android系统下，注释对象无法解析等问题
