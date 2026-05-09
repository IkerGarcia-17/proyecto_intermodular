package dam.iker.padeldart;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Repositorio local con los municipios de las 50 provincias españolas.
// Al ser datos estáticos en memoria, la carga es instantánea y funciona sin conexión.
// Fuente: nomenclátor del INE (dominio público). Se incluyen todos los municipios con
// más de 1.000 habitantes y los capitales de provincia.
// Si un municipio no está en la lista, el usuario puede elegir "✏️  Otra localidad…" al final.
public class MunicipiosEspana {

    // Mapa provincia → array de municipios. Inicializado una sola vez al cargar la clase.
    private static final Map<String, String[]> DATA = new HashMap<>();

    static {
        // ── ÁLAVA ────────────────────────────────────────────────────────────
        DATA.put("Álava", new String[]{
            "Vitoria-Gasteiz","Llodio","Amurrio","Laudio","Artziniega",
            "Arceniega","Agurain/Salvatierra","Laguardia","Labastida",
            "Lapuebla de Labarca","Elciego","Baños de Ebro","Elvillar","Navaridas",
            "Oyón-Oion","Kripan","Yécora","Moreda de Álava","Bernedo",
            "Campezo/Kanpezu","Peñacerrada-Urizaharra","Harana/Valle de Arana",
            "Asparrena","Dulantzi","Iruraiz-Gauna","Barrundia","Zuia",
            "Zigoitia","Legutio","Okondo","Urkabustaiz","Kuartango"
        });

        // ── ALBACETE ─────────────────────────────────────────────────────────
        DATA.put("Albacete", new String[]{
            "Albacete","Hellín","Almansa","La Roda","Villarrobledo",
            "Caudete","Tobarra","Yeste","Elche de la Sierra","Mahora",
            "Tarazona de la Mancha","Casas-Ibáñez","Chinchilla de Monte-Aragón",
            "Alcalá del Júcar","Fuentealbilla","La Herrera","Madrigueras",
            "Minaya","Motilleja","Munera","Ontur","Pozo-Lorente",
            "Pozohondo","Pozuelo","Robledo","Viveros","Bonillo",
            "El Ballestero","Férez","Liétor","Nerpio","Peñas de San Pedro",
            "Riópar","Socovos","Vianos","Villatoya","Bonete"
        });

        // ── ALICANTE ─────────────────────────────────────────────────────────
        DATA.put("Alicante", new String[]{
            "Alicante","Elche","Torrevieja","Orihuela","Benidorm",
            "Alcoy","Elda","Petrer","San Vicente del Raspeig","Villena",
            "Crevillent","Santa Pola","Guardamar del Segura","Pilar de la Horadada",
            "San Fulgencio","Los Montesinos","Rojales","Algorfa",
            "Bigastro","Callosa de Segura","Catral","Cox","Dolores",
            "Granja de Rocamora","Redován","San Isidro","Almoradí",
            "Benejúzar","Benijófar","Formentera del Segura","Jacarilla",
            "Rafal","Villamartín","Daya Vieja","Dénia","Calpe","Jávea",
            "Altea","Alfaz del Pi","Finestrat","Villajoyosa","El Campello",
            "Mutxamel","Novelda","Monóvar","Aspe","Sax","La Romana",
            "Agost","Biar","Castalla","Ibi","Onil","Tibi","Cocentaina",
            "Muro de Alcoy","Benifallim","Penàguila","Beniarrés","Alcosser",
            "Gorga","Quatretondeta","Fageca","Famorca","Tollos","Alcoleja"
        });

        // ── ALMERÍA ──────────────────────────────────────────────────────────
        DATA.put("Almería", new String[]{
            "Almería","El Ejido","Níjar","Roquetas de Mar","Vícar",
            "Berja","Adra","Huércal de Almería","Gádor","Huércal-Overa",
            "Cuevas del Almanzora","Vera","Carboneras","Mojácar","Garrucha",
            "Pulpí","Oria","Vélez Rubio","Vélez Blanco","Olula del Río",
            "Macael","Baza","Purchena","Tabernas","Gérgal","Santa Fe de Mondújar",
            "Pechina","Rioja","Terque","Alhabia","Instinción","Rágol",
            "Bentarique","Alsodux","Illar","Padules","Canjáyar","Ohanes","Beires"
        });

        // ── ASTURIAS ─────────────────────────────────────────────────────────
        DATA.put("Asturias", new String[]{
            "Oviedo","Gijón","Avilés","Siero","Langreo","Mieres",
            "Llanera","Castrillón","Carreño","Gozón","Corvera de Asturias",
            "Las Regueras","Noreña","Sariego","Nava","Caso","Piloña","Parres",
            "Cangas de Onís","Llanes","Ribadesella","Colunga","Villaviciosa",
            "Cabrales","Peñamellera Baja","Peñamellera Alta","Ribadedeva",
            "Valdés","Tineo","Grado","Salas","Pravia","Muros de Nalón",
            "Cudillero","Navia","Coaña","El Franco","Tapia de Casariego",
            "Castropol","Vegadeo","Illano","San Tirso de Abres","Villanueva de Oscos",
            "Pesoz","Santa Eulalia de Oscos","Grandas de Salime","Ibias","Degaña",
            "Cangas del Narcea","Allande","Tineo","Belmonte de Miranda"
        });

        // ── ÁVILA ────────────────────────────────────────────────────────────
        DATA.put("Ávila", new String[]{
            "Ávila","Arenas de San Pedro","Candeleda","Sotillo de la Adrada",
            "Mombeltrán","Piedralaves","El Tiemblo","Cebreros","Burgohondo",
            "Navarredonda de Gredos","Hoyos del Espino","Muñana","Sotalbo",
            "Gemuño","Papatrigo","Mingorría","La Colilla","Vicolozano",
            "Bernuy-Zapardiel","Fontiveros","San Pedro del Arroyo","Gotarrendura",
            "El Fresno","Niharra","Aldeaseca","Berrocalejo de Aragona",
            "Las Navas del Marqués","Peguerinos","San Bartolomé de Pinares",
            "Hoyo de Pinares","El Barraco","Navatalgordo","Navaluenga"
        });

        // ── BADAJOZ ──────────────────────────────────────────────────────────
        DATA.put("Badajoz", new String[]{
            "Badajoz","Mérida","Don Benito","Villanueva de la Serena",
            "Almendralejo","Zafra","Azuaga","Llerena","Jerez de los Caballeros",
            "Montijo","Olivenza","Barcarrota","Los Santos de Maimona",
            "Fuente de Cantos","Granja de Torrehermosa","Castuera","Cabeza del Buey",
            "Puebla de Alcocer","Herrera del Duque","Talarrubias","Campanario",
            "Quintana de la Serena","La Coronada","Zalamea de la Serena",
            "Monterrubio de la Serena","Malpartida de la Serena","Higuera de la Serena",
            "Medellín","Guareña","Mirandilla","Carrascalejo","Aljucén"
        });

        // ── BALEARES ─────────────────────────────────────────────────────────
        DATA.put("Baleares", new String[]{
            "Palma","Calvià","Marratxí","Llucmajor","Manacor","Inca",
            "Felanitx","Santanyí","Ses Salines","Campos","Porreres",
            "Petra","Sant Joan","Sineu","Costitx","Sencelles","Binissalem",
            "Lloseta","Selva","Campanet","Sa Pobla","Pollença","Alcúdia",
            "Muro","Santa Margalida","Artà","Capdepera","Son Servera",
            "Sant Llorenç des Cardassar","Algaida","Montuïri","Vilafranca de Bonany",
            "Puigpunyent","Esporles","Valldemossa","Deià","Sóller","Fornalutx",
            "Bunyola","Santa Maria del Camí","Consell","Alaró","Mancor de la Vall",
            "Mahón","Ciutadella","Ferreries","Es Mercadal","Es Migjorn Gran",
            "Es Castell","Alaior","Sant Lluís","Es Castell",
            "Ibiza","Santa Eulalia del Río","San Antonio Abad","San José","San Juan Bautista",
            "Formentera"
        });

        // ── BARCELONA ────────────────────────────────────────────────────────
        DATA.put("Barcelona", new String[]{
            "Barcelona","L'Hospitalet de Llobregat","Badalona","Sabadell",
            "Terrassa","Santa Coloma de Gramenet","Mataró","Cornellà de Llobregat",
            "Sant Cugat del Vallès","Rubí","Castelldefels","Sant Adrià de Besòs",
            "Cerdanyola del Vallès","Granollers","Viladecans","Esplugues de Llobregat",
            "Mollet del Vallès","El Prat de Llobregat","Gavà","Manresa",
            "Igualada","Vic","Vilanova i la Geltrú","Martorell",
            "Barberà del Vallès","Montcada i Reixac","Sant Feliu de Llobregat",
            "Ripollet","Castellbisbal","Abrera","Olesa de Montserrat",
            "Sitges","Calella","Pineda de Mar","Malgrat de Mar","Premià de Mar",
            "Montgat","Masnou","Alella","Tiana","Caldes de Montbui",
            "Berga","Manlleu","Torelló","Sant Quirze del Vallès","Sentmenat",
            "Polinyà","Santa Perpètua de Mogoda","Molins de Rei","Sant Andreu de la Barca",
            "Pallejà","Vilafranca del Penedès","Cubelles","Cunit","Calafell",
            "Segur de Calafell","Sant Vicenç de Calders","Parets del Vallès",
            "Lliçà d'Amunt","Lliçà de Vall","Bigues i Riells","Sant Celoni",
            "Cardedeu","La Garriga","Granollers","Mollet del Vallès"
        });

        // ── BURGOS ───────────────────────────────────────────────────────────
        DATA.put("Burgos", new String[]{
            "Burgos","Miranda de Ebro","Aranda de Duero","Villarcayo",
            "Medina de Pomar","Briviesca","Lerma","Belorado",
            "Salas de los Infantes","Roa","Peñaranda de Duero","Covarrubias",
            "Santo Domingo de Silos","Espinosa de los Monteros","Trespaderne",
            "Frías","Oña","Pancorbo","Poza de la Sal","Castrojeriz",
            "Melgar de Fernamental","Villasandino","Astudillo","Palenzuela",
            "Valle de Mena","Merindad de Sotoscueva","Merindad de Valdeporres",
            "Treviana","Cerezo de Río Tirón","Belorado","Ibeas de Juarros",
            "Hurones","Rabé de las Calzadas","Castrillo del Val"
        });

        // ── CÁCERES ──────────────────────────────────────────────────────────
        DATA.put("Cáceres", new String[]{
            "Cáceres","Plasencia","Navalmoral de la Mata","Coria","Trujillo",
            "Miajadas","Montánchez","Valencia de Alcántara","Zarza de Granadilla",
            "Hervás","Jerte","Cabezuela del Valle","Piornal","Tornavacas",
            "Moraleja","Alcántara","Brozas","Zarza la Mayor","Piedras Albas",
            "Aliseda","Arroyo de la Luz","Casar de Cáceres","Navas del Madroño",
            "Navas del Madroño","Garrovillas de Alconétar","Monroy",
            "Santiago de Alcántara","Membrío","Mata de Alcántara",
            "Talayuela","Rosalejo","Bohonal de Ibor","Valdecañas de Tajo",
            "Campillo de Deleitosa","Berzocana","Guadalupe","Logrosán"
        });

        // ── CÁDIZ ────────────────────────────────────────────────────────────
        DATA.put("Cádiz", new String[]{
            "Cádiz","Jerez de la Frontera","Algeciras","El Puerto de Santa María",
            "San Fernando","Chiclana de la Frontera","Sanlúcar de Barrameda",
            "La Línea de la Concepción","Rota","Arcos de la Frontera",
            "Conil de la Frontera","Vejer de la Frontera","Barbate",
            "Los Barrios","Tarifa","Jimena de la Frontera","Medina-Sidonia",
            "Puerto Real","Paterna de Rivera","Benaocaz","El Bosque",
            "Ubrique","Grazalema","Prado del Rey","El Gastor","Zahara de la Sierra",
            "Olvera","Setenil de las Bodegas","Torre Alháquime","Alcalá del Valle",
            "Algar","Bornos","Espera","Villamartín","Puerto Serrano"
        });

        // ── CANTABRIA ────────────────────────────────────────────────────────
        DATA.put("Cantabria", new String[]{
            "Santander","Torrelavega","Camargo","Castro-Urdiales",
            "El Astillero","Laredo","Reinosa","San Vicente de la Barquera",
            "Potes","Comillas","Suances","Miengo","Polanco",
            "Piélagos","Santa Cruz de Bezana","Cartes","Los Corrales de Buelna",
            "Santiurde de Reinosa","Cabezón de la Sal","San Miguel de Aguayo",
            "Valdeolea","Campoo de Enmedio","Valdeprado del Río","Pesquera",
            "Cabuérniga","Herrerías","Rionansa","Val de San Vicente",
            "Ruiloba","Udías","Alfoz de Lloredo","Valdaliga","Mazcuerras",
            "Molledo","Cieza","Entrambasaguas","Penagos","Santa María de Cayón"
        });

        // ── CASTELLÓN ────────────────────────────────────────────────────────
        DATA.put("Castellón", new String[]{
            "Castellón de la Plana","Vila-real","Burriana","Benicàssim",
            "Onda","Vinaròs","Peñíscola","Benicarló","Almassora",
            "Nules","La Vall d'Uixó","Segorbe","Morella","Albocàsser",
            "Torreblanca","Alcalà de Xivert","Oropesa del Mar","Cabanes",
            "Les Coves de Vinromà","Sant Jordi","Càlig","Traiguera",
            "La Jana","Cervera del Maestre","Canet lo Roig","Rossell",
            "Xert","La Salzadella","Santa Magdalena de Pulpis","Alcossebre",
            "Atzeneta del Maestrat","Benlloch","Coves de Vinromà","Tirig",
            "Culla","Benassal","Albocàsser","Vilafranca"
        });

        // ── CIUDAD REAL ──────────────────────────────────────────────────────
        DATA.put("Ciudad Real", new String[]{
            "Ciudad Real","Puertollano","Tomelloso","Manzanares","Valdepeñas",
            "Alcázar de San Juan","Daimiel","Miguelturra","Campo de Criptana",
            "Almadén","Socuéllamos","Pedro Muñoz","Herencia",
            "Villanueva de los Infantes","Almagro","Bolañas de Calatrava",
            "Argamasilla de Alba","Moral de Calatrava","Calzada de Calatrava",
            "Granátula de Calatrava","Aldea del Rey","Almedina","Alcubillas",
            "Infantes","Fuenllana","Montiel","Villahermosa","Solana del Pino",
            "La Solana","Alhambra","Cózar","La Membrilla"
        });

        // ── CÓRDOBA ──────────────────────────────────────────────────────────
        DATA.put("Córdoba", new String[]{
            "Córdoba","Lucena","Montilla","Puente Genil","Pozoblanco",
            "Baena","Cabra","Priego de Córdoba","Palma del Río",
            "Fernán Núñez","Montoro","Villa del Río","Bujalance",
            "Pedroche","Villanueva de Córdoba","Añora","Belalcázar",
            "Hinojosa del Duque","Peñarroya-Pueblonuevo","Fuente Obejuna",
            "Benamejí","Rute","Iznájar","Carcabuey","Zuheros",
            "Doña Mencía","Luque","Nueva Carteya","Espejo","Castro del Río",
            "Pedro Abad","Adamuz","Villafranca de Córdoba","El Carpio",
            "Alcaracejos","El Viso","Santa Eufemia","Cardeña"
        });

        // ── CUENCA ───────────────────────────────────────────────────────────
        DATA.put("Cuenca", new String[]{
            "Cuenca","Tarancón","San Clemente","Motilla del Palancar",
            "Villarobledo","Pedroñeras","La Almarcha","Quintanar del Rey",
            "Mota del Cuervo","Minglanilla","Iniesta","Priego",
            "Cañada del Hoyo","Cañamares","Buenache de Alarcón",
            "Honrubia","Las Valeras","Montalbo","Olivares de Júcar",
            "Palomares del Campo","Sisante","Vara de Rey","El Cañavate"
        });

        // ── GERONA ───────────────────────────────────────────────────────────
        DATA.put("Gerona", new String[]{
            "Girona","Figueres","Blanes","Lloret de Mar","Salt",
            "Palafrugell","Platja d'Aro","Olot","Banyoles","Ripoll",
            "Puigcerdà","Sant Feliu de Guíxols","Pals","Torroella de Montgrí",
            "L'Estartit","Cadaqués","Roses","L'Escala","Empúriabrava",
            "Palamós","Calonge","Santa Cristina d'Aro","Llagostera",
            "Quart","Fornells de la Selva","Riudellots de la Selva","Celrà",
            "Bàscara","Ventalló","Viladamat","Garrigàs","Riumors",
            "Castelló d'Empúries","Sant Pere Pescador","La Tallada d'Empordà"
        });

        // ── GRANADA ──────────────────────────────────────────────────────────
        DATA.put("Granada", new String[]{
            "Granada","Motril","Loja","Baza","Armilla","Almuñécar",
            "Huéscar","Guadix","Ogíjares","La Zubia","Maracena",
            "Cájar","Cenes de la Vega","Dílar","Gójar","Huétor Vega",
            "Monachil","Pinos Genil","Cúllar Vega","Vegas del Genil",
            "Churriana de la Vega","Las Gabias","Alhendín","Ventas de Huelma",
            "Iznalloz","Peza","Baza","Cullar","Caniles","Benamaurel",
            "Orce","Galera","Castilléjar","Cortes de Baza","Zújar",
            "Huéneja","Ferreira","Dólar","La Calahorra","Aldeire",
            "Capileira","Bubión","Pampaneira","Soportújar","Lanjarón",
            "Órgiva","Torvizcón","Alpujarra de la Sierra"
        });

        // ── GUADALAJARA ──────────────────────────────────────────────────────
        DATA.put("Guadalajara", new String[]{
            "Guadalajara","Azuqueca de Henares","Alovera","El Casar",
            "Marchamalo","Chiloeches","Cabanillas del Campo","Sigüenza",
            "Molina de Aragón","Brihuega","Pastrana","Sacedón","Torija",
            "Mondéjar","Yunquera de Henares","Horche","Valdeaveruelo",
            "Valdeaveruelo","Pioz","Lupiana","Romancos","Sayatón",
            "Auñón","Almonacid de Zorita","Albalate de Zorita","Entrepeñas"
        });

        // ── GUIPÚZCOA ────────────────────────────────────────────────────────
        DATA.put("Guipúzcoa", new String[]{
            "Donostia-San Sebastián","Irun","Errenteria","Eibar",
            "Zarautz","Hernani","Tolosa","Arrasate/Mondragón","Bergara",
            "Oñati","Beasain","Zumarraga","Azpeitia","Azkoitia",
            "Zumaia","Deba","Mutriku","Orio","Getaria","Zestoa",
            "Lasarte-Oria","Usurbil","Astigarraga","Pasaia","Lezo",
            "Oiartzun","Hondarribia","Andoain","Urnieta","Villabona",
            "Amasa-Villabona","Ibarra","Ordizia","Lazkao","Idiazabal",
            "Segura","Ormaiztegi","Legazpi","Zumarraga","Urretxu"
        });

        // ── HUELVA ───────────────────────────────────────────────────────────
        DATA.put("Huelva", new String[]{
            "Huelva","Almonte","Lepe","Ayamonte","Isla Cristina",
            "Moguer","Palos de la Frontera","Mazagón","Cartaya",
            "Punta Umbría","Aljaraque","San Juan del Puerto","Trigueros",
            "Beas","La Palma del Condado","Valverde del Camino",
            "Zalamea la Real","Aracena","Cortegana","Nerva","Minas de Riotinto",
            "Niebla","Lucena del Puerto","Rociana del Condado","Bollullos par del Condado",
            "La Redondela","El Rompido","Lepe","Cumbres Mayores","Cumbres de San Bartolomé",
            "Cumbres de Enmedio","Encinasola","Rosal de la Frontera",
            "Paymogo","El Almendro","Calañas","La Zarza-Perrunal"
        });

        // ── HUESCA ───────────────────────────────────────────────────────────
        DATA.put("Huesca", new String[]{
            "Huesca","Jaca","Barbastro","Monzón","Fraga","Sabiñánigo",
            "Binéfar","Graus","Boltaña","Aínsa","Biescas","Broto",
            "Torla-Ordesa","Benasque","Canfranc","Sallent de Gállego",
            "Benabarre","Grañén","Almudévar","Gurrea de Gállego",
            "Tardienta","Sariñena","Monegrillo","Bujaraloz","Osso de Cinca"
        });

        // ── JAÉN ─────────────────────────────────────────────────────────────
        DATA.put("Jaén", new String[]{
            "Jaén","Linares","Úbeda","Andújar","Alcalá la Real","Baeza",
            "Martos","Bailén","Torredonjimeno","Mancha Real","Mengíbar",
            "Guarromán","Villanueva del Arzobispo","La Carolina",
            "Navas de San Juan","Villarrodrigo","Peal de Becerro",
            "Cazorla","Quesada","Santiago-Pontones","Segura de la Sierra",
            "Arroyo del Ojanco","Génave","Hornos","Siles","Torres de Albánchez",
            "Beas de Segura","Chiclana de Segura","Villarrodrigo",
            "Iznatoraf","Villacarrillo","Castellar","Jódar","Huelma",
            "Cambil","Campillo de Arenas","Valdepeñas de Jaén"
        });

        // ── LA CORUÑA ────────────────────────────────────────────────────────
        DATA.put("La Coruña", new String[]{
            "A Coruña","Santiago de Compostela","Ferrol","Oleiros","Arteixo",
            "Culleredo","Cambre","Carballo","Betanzos","Narón","Fene",
            "Neda","As Pontes de García Rodríguez","Ortigueira","Cedeira",
            "Cee","Muros","Noia","Padrón","A Pobra do Caramiñal","Ribeira",
            "Boiro","Santa Comba","Negreira","Arzúa","Melide","Ordes",
            "Oroso","Teo","Ames","Rois","Dodro","Rianxo","Catoira",
            "Vilagarcía de Arousa","Lalín","Silleda","Vila de Cruces"
        });

        // ── LA RIOJA ─────────────────────────────────────────────────────────
        DATA.put("La Rioja", new String[]{
            "Logroño","Calahorra","Arnedo","Haro","Nájera","Alfaro",
            "Santo Domingo de la Calzada","Lardero","Villamediana de Iregua",
            "Alberite","Albelda de Iregua","Navarrete","Fuenmayor",
            "Cenicero","Briones","San Asensio","Baños de Rioja",
            "Casalarreina","Anguciana","Tirgo","Alesanco","Bañares",
            "Torremontalbo","Uruñuela","Bezares","Clavijo","Viguera",
            "Entrena","Sorzano","Medrano","Daroca de Rioja","Galilea",
            "Hornos de Moncalvillo","Logroño","Villarroya","Munilla",
            "Enciso","Arnedillo","Herce","Préjano","Aldeanueva de Ebro",
            "Rincón de Soto","Autol","Quel","Cervera del Río Alhama"
        });

        // ── LAS PALMAS ───────────────────────────────────────────────────────
        DATA.put("Las Palmas", new String[]{
            "Las Palmas de Gran Canaria","Telde","Santa Lucía de Tirajana",
            "San Bartolomé de Tirajana","Arucas","Ingenio","Agüimes",
            "Valsequillo de Gran Canaria","Teror","Gáldar","Guía de Gran Canaria",
            "Moya","Mogán","La Aldea de San Nicolás","Tejeda","Artenara",
            "Puerto del Rosario","Arrecife","Tías","San Bartolomé",
            "Tinajo","Teguise","Haría","Yaiza","Antigua",
            "Pájara","Tuineje","La Oliva","Corralejo"
        });

        // ── LEÓN ─────────────────────────────────────────────────────────────
        DATA.put("León", new String[]{
            "León","Ponferrada","San Andrés del Rabanedo","Villaquilambre",
            "Astorga","La Bañeza","Villablino","Bembibre","Fabero",
            "Cacabelos","Sahagún","Valencia de Don Juan","Cistierna",
            "Boñar","La Robla","Pola de Gordón","Matallana de Torío",
            "Sariegos","Onzonilla","Santovenia de la Valdoncina",
            "Valverde de la Virgen","Villadangos del Páramo",
            "San Justo de la Vega","Vega de Infanzones","Ardón",
            "Chozas de Abajo","Bustillo del Páramo","San Pedro Bercianos"
        });

        // ── LÉRIDA ───────────────────────────────────────────────────────────
        DATA.put("Lérida", new String[]{
            "Lleida","Tàrrega","Balaguer","Mollerussa","Cervera",
            "Alpicat","Torres de Segre","Almacelles","Alcarràs",
            "Bell-lloc d'Urgell","Bellpuig","Igualada","La Seu d'Urgell",
            "Sort","Tremp","Vielha e Mijaran","Pobla de Segur",
            "Pont de Suert","Agramunt","Artesa de Segre","Guissona",
            "Solsona","Cardona","Berga","Gironella"
        });

        // ── LUGO ─────────────────────────────────────────────────────────────
        DATA.put("Lugo", new String[]{
            "Lugo","Monforte de Lemos","Viveiro","Sarria","Vilalba",
            "Burela","Foz","Ribadeo","A Fonsagrada","Chantada",
            "O Páramo","Guitiriz","Guntín","Portomarín","Outeiro de Rei",
            "Friol","Begonte","Castro de Rei","Cospeito","A Pastoriza",
            "Abadín","Alfoz","Barreiros","Lourenzá","Mondoñedo",
            "O Valadouro","Xove","Cervo","O Vicedo","Ourol"
        });

        // ── MADRID ───────────────────────────────────────────────────────────
        DATA.put("Madrid", new String[]{
            "Madrid","Móstoles","Alcalá de Henares","Fuenlabrada","Leganés",
            "Getafe","Alcobendas","Torrejón de Ardoz","Parla","Alcorcón",
            "Pozuelo de Alarcón","Rivas-Vaciamadrid","Las Rozas de Madrid",
            "Aranjuez","Valdemoro","Coslada","San Sebastián de los Reyes",
            "Majadahonda","Boadilla del Monte","Collado Villalba",
            "Arganda del Rey","Navalcarnero","Galapagar","Tres Cantos",
            "Pinto","Torrelodones","Brunete","Ciempozuelos",
            "San Fernando de Henares","Colmenar Viejo","El Escorial",
            "Manzanares el Real","Alpedrete","Moralzarzal",
            "Villanueva de la Cañada","Villanueva del Pardillo","Guadarrama",
            "Soto del Real","Navacerrada","Chinchón","Humanes de Madrid",
            "Mejorada del Campo","Velilla de San Antonio","Paracuellos de Jarama",
            "Algete","Daganzo de Arriba","Meco","Torres de la Alameda",
            "Loeches","Nuevo Baztán","Ajalvir","Cobeña","El Molar",
            "Pedrezuela","Torrelaguna","Cercedilla","Los Molinos",
            "Colmenarejo","Villalbilla","Anchuelo","Campo Real",
            "San Martín de Valdeiglesias","Pelayos de la Presa","Méntrida",
            "Navalagamella","Quijorna","Sevilla la Nueva","Chapinería",
            "Moraleja de Enmedio","Arroyomolinos","Móstoles","Serranillos del Valle",
            "Casarrubuelos","Cubas de la Sagra","Griñón","Batres","Serranillos",
            "Titulcia","Villaconejos","Villamanrique de Tajo","Brea de Tajo",
            "Estremera","Fuentidueña de Tajo","Orusco de Tajuña","Belmonte de Tajo"
        });

        // ── MÁLAGA ───────────────────────────────────────────────────────────
        DATA.put("Málaga", new String[]{
            "Málaga","Marbella","Vélez-Málaga","Estepona","Benalmádena",
            "Fuengirola","Torremolinos","Mijas","Rincón de la Victoria",
            "Antequera","Nerja","Alhaurín de la Torre","Alhaurín el Grande",
            "Coín","Cártama","Pizarra","Álora","Archidona","Ronda",
            "Campillos","Manilva","Casares","Torrox","Algarrobo","Sedella",
            "Cómpeta","Frigiliana","Nerja","Canillas de Aceituno","Sayalonga",
            "Arenas","Alcaucín","Riogordo","Colmenar","Alfarnate",
            "Teba","Ardales","Carratraca","Almargen","Cañete la Real",
            "Cuevas Bajas","Cuevas de San Marcos","Villanueva de Tapia",
            "Humilladero","Mollina","Fuente de Piedra","Alameda",
            "La Roda de Andalucía","Sierra de Yeguas","Gobantes",
            "Benaoján","Montejaque","Jimera de Líbar","Cortes de la Frontera",
            "Gaucín","Genalguacil","Jubrique","Benarrabá","Algatocín"
        });

        // ── MURCIA ───────────────────────────────────────────────────────────
        DATA.put("Murcia", new String[]{
            "Murcia","Cartagena","Lorca","Molina de Segura","Alcantarilla",
            "El Palmar","Cieza","Yecla","Águilas","Jumilla",
            "San Pedro del Pinatar","San Javier","Los Alcázares",
            "Torre-Pacheco","Mazarrón","Totana","Alhama de Murcia",
            "Mula","Caravaca de la Cruz","Moratalla","Cehegín",
            "Calasparra","Archena","Campos del Río","Abarán",
            "Blanca","Ricote","Ojós","Lorquí","Alguazas",
            "Ceutí","Las Torres de Cotillas","Beniel","Santomera",
            "Fortuna","Abanilla","Fuente Álamo de Murcia",
            "Balsicas","Santiago de la Ribera","Cabo de Palos",
            "Corvera","Pozo Estrecho","Los Ramos","El Jimenado"
        });

        // ── NAVARRA ──────────────────────────────────────────────────────────
        DATA.put("Navarra", new String[]{
            "Pamplona","Tudela","Barañáin","Burlada/Burlata","Estella-Lizarra",
            "Alsasua/Altsasu","Tafalla","Zizur Mayor/Zizur Nagusia",
            "Villava","Ansoáin","Berriozar","Huarte/Uharte","Sarriguren",
            "Noáin","Orkoien","Berrioplano","Sangüesa","Aoiz","Lumbier",
            "Carcastillo","Arguedas","Cadreita","Valtierra","Milagro",
            "Peralta","Falces","Funes","Marcilla","Azagra",
            "Lodosa","Andosilla","Sartaguda","Cárcar","Larraga",
            "Berbinzana","Miranda de Arga","Artajona","Mendigorría",
            "Olite/Erriberri","Beire","San Martín de Unx","Ujué"
        });

        // ── ORENSE ───────────────────────────────────────────────────────────
        DATA.put("Orense", new String[]{
            "Ourense","Verín","O Barco de Valdeorras","O Carballiño",
            "Xinzo de Limia","Ribadavia","A Rúa","A Gudiña","Celanova",
            "Allariz","Maceda","Bande","A Merca","Pereiro de Aguiar",
            "Coles","Barbadás","San Cibrao das Viñas","Toén",
            "Leiro","Castrelo de Miño","Arnoia","Cenlle","Carballeda de Avia",
            "Cortegada","Melón","Boborás","O Irixo","Beariz"
        });

        // ── PALENCIA ─────────────────────────────────────────────────────────
        DATA.put("Palencia", new String[]{
            "Palencia","Guardo","Aguilar de Campoo","Venta de Baños",
            "Paredes de Nava","Carrión de los Condes","Cervera de Pisuerga",
            "Villamuriel de Cerrato","Dueñas","Herrera de Pisuerga",
            "Saldaña","Sahagún","Ampudia","Astudillo","Baltanás",
            "Becerril de Campos","Espinosa de Cerrato","Frechilla",
            "Palenzuela","Valle del Retortillo","Buenavista de Valdavia"
        });

        // ── PONTEVEDRA ───────────────────────────────────────────────────────
        DATA.put("Pontevedra", new String[]{
            "Pontevedra","Vigo","Vilagarcía de Arousa","Sanxenxo",
            "Redondela","Moaña","Cangas","O Grove","A Illa de Arousa",
            "Cambados","O Porriño","Tui","A Guarda","Baiona",
            "Nigrán","Gondomar","Mos","Salceda de Caselas","As Neves",
            "Covelo","Creciente","Arbo","A Cañiza","Covelo",
            "Cañiza","Fornelos de Montes","Avión","Beariz","Melón",
            "Dozón","A Estrada","Silleda","Vila de Cruces","Cuntis",
            "Caldas de Reis","Valga","Catoira","Rianxo","Boiro"
        });

        // ── SALAMANCA ────────────────────────────────────────────────────────
        DATA.put("Salamanca", new String[]{
            "Salamanca","Béjar","Ciudad Rodrigo","Vitigudino","Santa Marta de Tormes",
            "Peñaranda de Bracamonte","Carbajosa de la Sagrada","Cabrerizos",
            "Villares de la Reina","San Cristóbal de la Cuesta","Villamayor",
            "Alba de Tormes","Guijuelo","Tamames","Sequeros","La Alberca",
            "Miranda del Castañar","Candelario","Montemayor del Río",
            "Hervás","Baños de Montemayor","El Tejado","Navarredonda de la Rinconada",
            "Vecinos","Monterrubio de la Sierra","Palaciosrubios","Morille"
        });

        // ── SANTA CRUZ DE TENERIFE ───────────────────────────────────────────
        DATA.put("Santa Cruz de Tenerife", new String[]{
            "Santa Cruz de Tenerife","San Cristóbal de La Laguna",
            "Arona","Adeje","Granadilla de Abona","Los Llanos de Aridane",
            "El Rosario","Güímar","Candelaria","Arafo","Tacoronte",
            "La Victoria de Acentejo","La Matanza de Acentejo","El Sauzal",
            "Tegueste","Santa Úrsula","La Orotava","Puerto de la Cruz",
            "Los Realejos","Icod de los Vinos","Garachico","Buenavista del Norte",
            "Santiago del Teide","Guía de Isora","San Miguel de Abona",
            "Valle Gran Rey","Vallehermoso","Hermigua","Alajeró",
            "San Sebastián de La Gomera","Tijarafe","Breña Alta","Breña Baja",
            "Fuencaliente de la Palma","Los Sauces","Santa Cruz de la Palma",
            "El Paso","Puntagorda","Puntallana","Garafía","Barlovento"
        });

        // ── SEGOVIA ──────────────────────────────────────────────────────────
        DATA.put("Segovia", new String[]{
            "Segovia","El Espinar","San Ildefonso","Cuéllar","Riaza",
            "Sepúlveda","Coca","Nava de la Asunción","Palazuelos de Eresma",
            "La Granja de San Ildefonso","Villacastín","Cantalejo",
            "Carbonero el Mayor","Santa María la Real de Nieva","Turégano",
            "Navafría","El Muyo","Navares de las Cuevas","Aldehorno",
            "Villar de Sobrepeña","Castillejo de Mesleón","Somosierra"
        });

        // ── SEVILLA ──────────────────────────────────────────────────────────
        DATA.put("Sevilla", new String[]{
            "Sevilla","Dos Hermanas","Alcalá de Guadaíra","Mairena del Aljarafe",
            "Utrera","San Juan de Aznalfarache","Carmona","La Rinconada",
            "Écija","Marchena","Morón de la Frontera","Coria del Río",
            "Osuna","Bormujos","Tomares","Camas","Palomares del Río",
            "La Algaba","Guillena","Gerena","Salteras","Espartinas",
            "Umbrete","Olivares","Sanlúcar la Mayor","Huévar del Aljarafe",
            "Benacazón","Castilleja de la Cuesta","Gines","Mairena del Alcor",
            "El Viso del Alcor","Los Palacios y Villafranca","Las Cabezas de San Juan",
            "Lebrija","Constantina","El Pedroso","Cantillana","Lora del Río",
            "Peñaflor","Alcalá del Río","Brenes","Villaverde del Río",
            "Tocina","La Puebla del Río","Isla Mayor","Aznalcóllar",
            "Aznalcázar","Pilas","Villamanrique de la Condesa",
            "Almensilla","Gelves","Puebla del Río","Coria del Río"
        });

        // ── SORIA ────────────────────────────────────────────────────────────
        DATA.put("Soria", new String[]{
            "Soria","El Burgo de Osma","Ólvega","Ágreda","Almazán",
            "Medinaceli","San Leonardo de Yagüe","Arcos de Jalón",
            "Berlanga de Duero","Covaleda","Vinuesa","Molinos de Duero",
            "Cidones","Garray","Los Rábanos","Golmayo"
        });

        // ── TARRAGONA ────────────────────────────────────────────────────────
        DATA.put("Tarragona", new String[]{
            "Tarragona","Reus","Tortosa","Salou","Cambrils","Vila-seca",
            "Constantí","Valls","Calafell","Vendrell","Cunit",
            "Montroig del Camp","Miami Platja","Vandellòs i l'Hospitalet de l'Infant",
            "L'Ametlla de Mar","Amposta","Deltebre","Sant Carles de la Ràpita",
            "Aldea","Alcanar","Ulldecona","La Sènia","Mas de Barberans",
            "Roquetes","Jesús","Aldover","Tivenys","Benifallet",
            "Paüls","Tivissa","Garcia","Rasquera","Móra d'Ebre","Gandesa",
            "Bot","Corbera d'Ebre","Batea","El Pinell de Brai","Vilalba dels Arcs"
        });

        // ── TERUEL ───────────────────────────────────────────────────────────
        DATA.put("Teruel", new String[]{
            "Teruel","Alcañiz","Andorra","Utrillas","Calamocha",
            "Híjar","Albalate del Arzobispo","Muniesa","Montalbán",
            "Alcorisa","Calanda","Mas de las Matas","La Puebla de Híjar",
            "Azaila","Vinaceite","Samper de Calanda","Castelnou"
        });

        // ── TOLEDO ───────────────────────────────────────────────────────────
        DATA.put("Toledo", new String[]{
            "Toledo","Talavera de la Reina","Illescas","Añover de Tajo",
            "Seseña","Yuncos","Numancia de la Sagra","Olías del Rey",
            "Bargas","Mocejón","Nambroca","Burguillos de Toledo",
            "Sonseca","Orgaz","Madridejos","Tembleque","Quintanar de la Orden",
            "Villacañas","Consuegra","Cabañas de la Sagra","Esquivias",
            "Yuncler","Yunclillos","Recas","Cobeja","Pantoja",
            "Villaluenga de la Sagra","Cedillo del Condado","Carranque",
            "Chozas de Canales","Rielves","Santa Cruz del Retamar",
            "Escalona","Maqueda","Torrijos","Santa Olalla","Fuensalida",
            "Cebolla","Cazalegas","La Pueblanueva","El Casar de Escalona"
        });

        // ── VALENCIA ─────────────────────────────────────────────────────────
        DATA.put("Valencia", new String[]{
            "Valencia","Torrent","Gandía","Paterna","Burjassot",
            "Sagunto","Mislata","Quart de Poblet","Alzira","Xirivella",
            "Aldaia","Ontinyent","Xàtiva","Catarroja","Manises",
            "Paiporta","Picanya","Alaquàs","Silla","Tavernes de la Valldigna",
            "Sueca","Cullera","Oliva","Dénia","Massanassa","Albal",
            "Alcàsser","Montserrat","Sedaví","Alfafar","Benetússer",
            "Foios","Museros","Meliana","Alboraya","Almàssera","Vinalesa",
            "Pobla de Farnals","Puçol","Rafelbunyol","Rocafort","Godella",
            "Moncada","Bétera","Náquera","Serra","Llíria","Villar del Arzobispo",
            "Requena","Utiel","Chiva","Cheste","Buñol","Turís",
            "Riba-roja de Túria","L'Eliana","Paterna","Pobla de Vallbona",
            "Benaguasil","Benissanó","San Antonio de Benagéber"
        });

        // ── VALLADOLID ───────────────────────────────────────────────────────
        DATA.put("Valladolid", new String[]{
            "Valladolid","Arroyo de la Encomienda","Laguna de Duero",
            "Medina del Campo","Peñafiel","Tordesillas","Cigales",
            "Simancas","Olmedo","Iscar","Portillo","Mojados",
            "Aldeamayor de San Martín","Boecillo","Zaratán","Cistérniga",
            "Fuensaldaña","Mucientes","Renedo de Esgueva","Tudela de Duero",
            "Serrada","Medina de Rioseco","Torrelobatón","Urueña"
        });

        // ── VIZCAYA ──────────────────────────────────────────────────────────
        DATA.put("Vizcaya", new String[]{
            "Bilbao","Barakaldo","Getxo","Portugalete","Santurtzi",
            "Basauri","Leioa","Sestao","Galdakao","Durango",
            "Gernika-Lumo","Erandio","Berango","Sopelana","Urduliz",
            "Plentzia","Gorliz","Mungia","Derio","Zamudio","Sondika",
            "Laukiz","Gatika","Bakio","Bermeo","Mundaka","Forua",
            "Muxika","Ondarroa","Lekeitio","Markina-Xemein","Zaldibar",
            "Ermua","Eibar","Bergara","Arrasate/Mondragón","Orozko",
            "Zeberio","Areatza","Igorre","Amorebieta-Etxano","Lemoa",
            "Bedia","Galdames","Güeñes","Zalla","Balmaseda","Gordexola",
            "Arcentales","Carranza","Trucíos-Turtzioz","Sopuerta","Muskiz"
        });

        // ── ZAMORA ───────────────────────────────────────────────────────────
        DATA.put("Zamora", new String[]{
            "Zamora","Benavente","Toro","Villanueva del Campo","Mombuey",
            "Alcañices","Puebla de Sanabria","Corrales del Vino",
            "Bermillo de Sayago","Villaralbo","Morales del Vino",
            "San Cristóbal de Entreviñas","Castroverde de Campos",
            "Villalpando","Fuentesaúco","El Perdigón","Entrala",
            "Fresno de la Ribera","Malva","Roales","Vidayanes"
        });

        // ── ZARAGOZA ─────────────────────────────────────────────────────────
        DATA.put("Zaragoza", new String[]{
            "Zaragoza","Calatayud","Utebo","Ejea de los Caballeros",
            "Tarazona","La Almunia de Doña Godina","Caspe","Borja",
            "Gallur","Alagón","Figueruelas","Fuentes de Ebro",
            "Pastriz","Alfajarín","María de Huerva","Cadrete",
            "Cuarte de Huerva","La Puebla de Alfindén","Villamayor de Gállego",
            "San Mateo de Gállego","Zuera","Luna","Tauste","Sádaba",
            "Mallén","Botorrita","Jaulín","Mozota","Longares",
            "Fuendetodos","Azuara","Belchite","Quinto","Pina de Ebro",
            "Gelsa","Velilla de Ebro","La Zaida","Escatrón","Castelnou"
        });
    }

    // Devuelve la lista de municipios de una provincia, ordenada alfabéticamente.
    // Al final añade la opción de escritura manual para municipios no catalogados.
    public static List<String> obtenerMunicipios(String provincia) {
        String[] arr = DATA.get(provincia);
        List<String> lista = arr != null
                ? new ArrayList<>(Arrays.asList(arr))
                : new ArrayList<>();
        // Ordenamos ignorando mayúsculas y acentos
        Collections.sort(lista, (a, b) -> {
            String na = a.toLowerCase(java.util.Locale.forLanguageTag("es"));
            String nb = b.toLowerCase(java.util.Locale.forLanguageTag("es"));
            return na.compareToIgnoreCase(nb);
        });
        // Opción al final para municipios no incluidos en la lista
        lista.add("✏️  Otra localidad...");
        return lista;
    }
}
