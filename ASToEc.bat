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

xcopy ..\CoreWiseSDK\IDCardSDK\src\main\AndroidManifest.xml .\IDCardSDK /s /y

xcopy ..\CoreWiseSDK\IDCardSDK\src\main\java .\IDCardSDK\src /s /e /c /y /h /r
xcopy ..\CoreWiseSDK\IDCardSDK\src\main\res .\IDCardSDK\res /s /e /c /y /h /r

xcopy ..\CoreWiseSDK\IDCardSDK\src\main\jnilibs .\IDCardSDK\libs /s /e /c /y /h /r

xcopy ..\CoreWiseSDK\IDCardSDK\libs .\IDCardSDK\libs /s /e /c /y /h /r


echo ------------------End！---------------

pause