@echo on

docker -v || echo ''
dotnet --info || echo ''
msbuild || echo ''
nuget --help || echo ''
python --version || echo ''
where python.exe || echo ''
where python2.exe || echo ''
where python3.exe || echo ''
vswhere python.exe || echo ''
py -2 --version || echo ''
py -3 --version || echo ''
vswhere || echo ''