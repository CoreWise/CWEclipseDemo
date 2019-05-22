@echo off
echo ------------------前方高能！一片绿！---------------
echo ------------------开始使用bat脚本将 AS copy to Eclipse!---------------
echo .
color 2e

echo ******************开始Copy SerialPortSDK ******************

xcopy ..\CoreWiseSDK\SerialPortSDK\src\main\AndroidManifest.xml .\SerialPortSDK /s /y

xcopy ..\CoreWiseSDK\SerialPortSDK\src\main\java .\SerialPortSDK\src /s /e /c /y /h /r
xcopy ..\CoreWiseSDK\SerialPortSDK\src\main\res .\SerialPortSDK\res /s /e /c /y /h /r

xcopy ..\CoreWiseSDK\SerialPortSDK\src\main\jnilibs .\SerialPortSDK\libs /s /e /c /y /h /r

xcopy ..\CoreWiseSDK\SerialPortSDK\libs .\SerialPortSDK\libs /s /e /c /y /h /r

echo .

echo ******************开始Copy IDCardSDK ******************

xcopy ..\CoreWiseSDK\IDCardSDK\src\main\AndroidManifest.xml .\IDCardSDK /s /y

xcopy ..\CoreWiseSDK\IDCardSDK\src\main\java .\IDCardSDK\src /s /e /c /y /h /r
xcopy ..\CoreWiseSDK\IDCardSDK\src\main\res .\IDCardSDK\res /s /e /c /y /h /r

xcopy ..\CoreWiseSDK\IDCardSDK\src\main\jnilibs .\IDCardSDK\libs /s /e /c /y /h /r

xcopy ..\CoreWiseSDK\IDCardSDK\libs .\IDCardSDK\libs /s /e /c /y /h /r

echo .

echo ******************开始Copy HXUHF ******************

xcopy ..\CoreWiseSDK\HXUHFSDK\src\main\AndroidManifest.xml .\HXUHFSDK /s /y

xcopy ..\CoreWiseSDK\HXUHFSDK\src\main\java .\HXUHFSDK\src /s /e /c /y /h /r
xcopy ..\CoreWiseSDK\HXUHFSDK\src\main\res .\HXUHFSDK\res /s /e /c /y /h /r

xcopy ..\CoreWiseSDK\HXUHFSDK\src\main\jnilibs .\HXUHFSDK\libs /s /e /c /y /h /r

xcopy ..\CoreWiseSDK\HXUHFSDK\libs .\HXUHFSDK\libs /s /e /c /y /h /r


echo ******************开始Copy BeiDouSDK ******************

xcopy ..\CoreWiseSDK\BeiDouSDK\src\main\AndroidManifest.xml .\BeiDouSDK /s /y

xcopy ..\CoreWiseSDK\BeiDouSDK\src\main\java .\BeiDouSDK\src /s /e /c /y /h /r
xcopy ..\CoreWiseSDK\BeiDouSDK\src\main\res .\BeiDouSDK\res /s /e /c /y /h /r

xcopy ..\CoreWiseSDK\BeiDouSDK\src\main\jnilibs .\BeiDouSDK\libs /s /e /c /y /h /r

xcopy ..\CoreWiseSDK\BeiDouSDK\libs .\BeiDouSDK\libs /s /e /c /y /h /r

echo ******************开始Copy BarCodeSDK ******************

xcopy ..\CoreWiseSDK\BarCodeSDK\src\main\AndroidManifest.xml .\BarCodeSDK /s /y

xcopy ..\CoreWiseSDK\BarCodeSDK\src\main\java .\BarCodeSDK\src /s /e /c /y /h /r
xcopy ..\CoreWiseSDK\BarCodeSDK\src\main\res .\BarCodeSDK\res /s /e /c /y /h /r

xcopy ..\CoreWiseSDK\BarCodeSDK\src\main\jnilibs .\BarCodeSDK\libs /s /e /c /y /h /r

xcopy ..\CoreWiseSDK\BarCodeSDK\libs .\BarCodeSDK\libs /s /e /c /y /h /r

echo ******************开始Copy M1RFIDSDK ******************

xcopy ..\CoreWiseSDK\M1RFIDSDK\src\main\AndroidManifest.xml .\M1RFIDSDK /s /y

xcopy ..\CoreWiseSDK\M1RFIDSDK\src\main\java .\M1RFIDSDK\src /s /e /c /y /h /r
xcopy ..\CoreWiseSDK\M1RFIDSDK\src\main\res .\M1RFIDSDK\res /s /e /c /y /h /r

xcopy ..\CoreWiseSDK\M1RFIDSDK\src\main\jnilibs .\M1RFIDSDK\libs /s /e /c /y /h /r

xcopy ..\CoreWiseSDK\M1RFIDSDK\libs .\M1RFIDSDK\libs /s /e /c /y /h /r

echo ******************开始Copy R2000UHFSDK ******************

xcopy ..\CoreWiseSDK\R2000UHFSDK\src\main\AndroidManifest.xml .\R2000UHFSDK /s /y

xcopy ..\CoreWiseSDK\R2000UHFSDK\src\main\java .\R2000UHFSDK\src /s /e /c /y /h /r
xcopy ..\CoreWiseSDK\R2000UHFSDK\src\main\res .\R2000UHFSDK\res /s /e /c /y /h /r

xcopy ..\CoreWiseSDK\R2000UHFSDK\src\main\jnilibs .\R2000UHFSDK\libs /s /e /c /y /h /r

xcopy ..\CoreWiseSDK\R2000UHFSDK\libs .\R2000UHFSDK\libs /s /e /c /y /h /r

echo ------------------一片绿End！---------------


pause