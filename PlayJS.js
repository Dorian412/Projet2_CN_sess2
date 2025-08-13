function createGridFromString(MPgrid){
    if(MPgrid)
        MPgrid = MPgrid.replace(/(\r\n)/g, '').replace(/(\n)/g, '');
        //on utilise pas replace sur "\r\n" sinon ca remplacerait que la premiere ocurence de \r\n
    
    if(MPgrid == "null")
        MPgrid = null;

    const grid = document.getElementById('minesweeper-grid');
    grid.innerHTML = '';
    for(let i = 0; i < 49; i++){
        const cell = document.createElement('div');
        cell.classList.add('cell');
        
        const row = Math.floor(i / 7);
        const col = i % 7;

        cell.dataset.row = row;
        cell.dataset.col = col;

        if(MPgrid){
            cell.dataset.value = MPgrid[i];

            if(MPgrid[i] != '#' && MPgrid[i] != 'F' && MPgrid[i] != 'B'){
                cell.textContent = MPgrid[i];
            }
            switch(MPgrid[i]){
                case '#':
                    cell.style.backgroundColor = 'lightgray';
                    break;
                case '1':
                    cell.style.color = 'blue';
                    cell.style.backgroundColor = 'white';
                    break;
                case '2':
                    cell.style.color = 'red';
                    cell.style.backgroundColor = 'white';
                    break;
                case '3':
                    cell.style.color = 'orange';
                    cell.style.backgroundColor = 'white';
                    break;
                case '4':
                    cell.style.color = 'green';
                    cell.style.backgroundColor = 'white';
                    break;
                case '5':
                    cell.style.color = 'yellow';
                    cell.style.backgroundColor = 'white';
                    break;
                case '6':
                    cell.style.color = 'blue';
                    cell.style.backgroundColor = 'white';
                    break;
                case '7':
                    cell.style.color = 'gray';
                    cell.style.backgroundColor = 'white';
                    break;
                case '8':
                    cell.style.color = 'darkblue';
                    cell.style.backgroundColor = 'white';
                    break;
                case 'B':
                    cell.classList.add('bomb');
                    cell.style.backgroundColor = 'white';
                    break;
                case 'F':
                    cell.classList.add('flag');
                    break;
                default:
                    cell.style.color = 'black';
                    cell.style.backgroundColor = 'white';
                    break;
            }
        }
        else {
            cell.dataset.value = "";
        }
        
        cell.onclick = () => revealCell(cell);
        cell.oncontextmenu = (event) => flagCell(event, cell);

        grid.appendChild(cell);
    }
}
function revealCell(cell){
    //console.log(cell.dataset.row);
    //console.log(cell.dataset.col);
    socket.send("TRY " + cell.dataset.row + " " + cell.dataset.col);
}
function flagCell(event, cell){
    event.preventDefault(); 
    cell.classList.toggle('flag');
    socket.send("FLAG " + cell.dataset.row + " " + cell.dataset.col);
}

//console.log(game);
createGridFromString(game);

const socket = new WebSocket("ws://localhost:8014/webSocket");

socket.onopen = function (){
    console.log("websocket connecté");
}

socket.onmessage = function(event){
    //console.log("grille recue " + event.data);
    createGridFromString(event.data);
}
