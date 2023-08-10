# SyncGallery
Sincronizza foto e video del tuo smartphone Android su di un server SMB remoto!

L'applicazione, presenta 4 bottoni sullo schermo, bottone (1) per la copia delle foto/video sulla directory DCIM/SYNC, bottone (2) per lo spostamento delle foto/video sulla directory DCIM/SYNC,  bottone (3) per cambiare directory sorgente, bottone (4) per la sincronizzazione delle foto/video da DCIM/SYNC al server SMB remoto.

Bottone 1:
Estrapola, dalle directory spceificate, tutte le foto e i video presenti e li copia su di una cartella singola chiamata SYNC.

Bottone 2:
Estrapola, dalle directory spceificate, tutte le foto e i video presenti e li taglia su di una cartella singola chiamata SYNC.

Bottone 3:
Permette di cambiare la directory sorgente.

Bottone 4:
Partendo dalla cartella SYNC, estrapola tutti i file al suo interno (foto/video) e li copia su di una cartella di rete remota, ospitata da un server SMB.


ATTENZIONE: L'applicazione Android, rappresenta solo una parte del progetto, si può creare un server SMB su di un Raspberry pi a cui è collegata una memoria esterna che raccoglie tutti i file (foto/video) sul server e li copia sulla memoria esterna collegata al raspberry.
In questo modo ottieni sistema di Backup di file, in questo caso foto e video, del tuo smartphone android, su di una memoria esterna e in maniera semi-automatica ed efficente.