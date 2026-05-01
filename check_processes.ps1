Get-CimInstance Win32_Process -Filter "ProcessId=37260 OR ProcessId=313436 OR ProcessId=14288" | Select-Object ProcessId, CommandLine | Format-List
