"""Own tiny GB ROMs exchange bytes through actual emulated serial hardware and TCP."""
from pathlib import Path
import subprocess, tempfile, sys
core, runner = map(lambda p:str(Path(p).resolve()),sys.argv[1:])

def rom(role, cgb=False):
    output=bytearray(32768); output[0x100:0x103]=bytes([0xC3,0x50,0x01])
    program=bytearray(); labels={}; jumps=[]
    def emit(*v): program.extend(v)
    def label(name): labels[name]=len(program)
    def jr(op,name): emit(op,0); jumps.append((len(program)-1,name))
    emit(0xF3,0xAF,0xE0,0x40) # disable interrupts and LCD
    emit(0x16,20 if role==0 else 1);label('d');emit(0x06,255);label('b');emit(0x0E,255);label('c')
    emit(0x0D);jr(0x20,'c');emit(0x05);jr(0x20,'b');emit(0x15);jr(0x20,'d')
    emit(0x3E,0x31 if role==0 else 0x72,0xE0,0x01)
    emit(0x3E,(0x83 if role==0 else 0x82) if cgb else (0x81 if role==0 else 0x80),0xE0,0x02)
    label('wait');emit(0xF0,0x02,0xCB,0x7F);jr(0x20,'wait')
    emit(0xF0,0x01,0xEA,0,0xC0,0x3E,0xA5,0xEA,1,0xC0)
    label('end');jr(0x18,'end')
    for offset,name in jumps: program[offset]=(labels[name]-offset-1)&255
    output[0x150:0x150+len(program)]=program
    output[0x134:0x13E]=b'KL SERIAL\0'; output[0x147]=0;output[0x148]=0;output[0x149]=0
    output[0x143] = 0x80 if cgb else 0
    checksum=0
    for b in output[0x134:0x14D]: checksum=(checksum-b-1)&255
    output[0x14D]=checksum
    return output
for cgb in (False, True):
    with tempfile.TemporaryDirectory(prefix='kl-gbc-core-' if cgb else 'kl-gb-core-') as tmp:
        root=Path(tmp); jobs=[]
        try:
            for role in (0,1):
                folder=root/str(role);folder.mkdir()
                (folder/'kl-link-session.cfg').write_text(f'{role} 127.0.0.1 '+ '0123456789abcdef'*2+'\n')
                fixture=folder/('host.gb' if role==0 else 'client.gb');fixture.write_bytes(rom(role,cgb))
                jobs.append(subprocess.Popen([runner,core,str(folder),str(fixture)],stdout=subprocess.DEVNULL,stderr=subprocess.PIPE))
            for role,process in enumerate(jobs):
                _,log=process.communicate(timeout=20)
                if process.returncode: raise RuntimeError(f'P{role}: {log.decode()}')
                print(f'{"GBC" if cgb else "GB"} P{role}: {log.decode().strip()}')
        finally:
            for p in jobs:
                if p.poll() is None:p.kill();p.wait()
