"""Genera app/src/main/assets/recetas.json: recetas españolas con ingredientes de la base genérica.
Cada ingrediente se busca por nombre exacto (o que empiece igual) entre los genéricos y se guardan sus valores
por 100 g, para que la app calcule sin depender de los ids del catálogo.
Uso (desde la raíz del proyecto): python datos/scripts/06_recetas.py"""
import json
import sqlite3
import sys

BD = 'app/src/main/assets/alimentos.db'
SALIDA = 'app/src/main/assets/recetas.json'
AOVE = "Aceite de oliva virgen extra"
PAN = "Pan blanco, de barra"

# comidas: D desayuno, M media mañana, C comida, R merienda, N cena
R = [
    ("Merluza al horno con patata", "🐟", "CN", 25, [("Merluza fresca", 200), ("Patata, cruda", 250), ("Cebolla", 60), (AOVE, 10)],
     ["Patata en rodajas finas con la cebolla al horno a 200 °C, 15 min.", "Añade la merluza con sal y limón, 10 min más."], ["Pescado"]),
    ("Pollo a la plancha con arroz y pimientos", "🍗", "CN", 20, [("Pollo, pechuga, con piel, crudo", 180), ("Arroz", 80), ("Pimiento rojo", 100), (AOVE, 10)],
     ["Cuece el arroz 15 min.", "Pollo y pimiento en tiras a la plancha con el aceite."], []),
    ("Lentejas estofadas con verduras", "🥣", "C", 40, [("Lenteja, seca, cruda", 80), ("Zanahoria", 60), ("Cebolla", 50), ("Tomate", 60), ("Patata, cruda", 100), (AOVE, 8)],
     ["Sofríe cebolla, zanahoria y tomate.", "Añade lentejas, patata y agua; 35 min a fuego medio."], []),
    ("Garbanzos con espinacas", "🥘", "CN", 15, [("Garbanzo, seco, crudo", 80), ("Espinaca", 150), ("Ajo", 5), (AOVE, 10), (PAN, 30)],
     ["Dora el ajo y el pan y tritúralos.", "Saltea las espinacas, añade garbanzos y el majado."], ["Gluten"]),
    ("Tortilla francesa con ensalada y pan", "🍳", "DCN", 10, [("Huevo de gallina fresco", 120), ("Lechuga", 80), ("Tomate", 100), (PAN, 50), (AOVE, 8)],
     ["Bate los huevos y cuaja la tortilla.", "Ensalada de lechuga y tomate aliñada."], ["Huevo", "Gluten"]),
    ("Ensalada de garbanzos con atún y huevo", "🥗", "CN", 10, [("Garbanzo, seco, crudo", 70), ("Atún, al natural", 60), ("Huevo de gallina, hervido, duro", 55), ("Tomate", 100), ("Cebolla", 30), (AOVE, 8)],
     ["Mezcla todo y aliña con aceite, vinagre y sal."], ["Pescado", "Huevo"]),
    ("Salmón al horno con judías verdes y patata", "🐟", "CN", 20, [("Salmón", 150), ("Judía verde, cruda", 200), ("Patata, cruda", 150), (AOVE, 5)],
     ["Patata troceada al horno 10 min.", "Añade salmón y brócoli, 12 min a 200 °C."], ["Pescado"]),
    ("Pasta con tomate y atún", "🍝", "CN", 15, [("Pasta alimenticia, cruda", 90), ("Tomate frito", 80), ("Atún, al natural", 60), ("Queso parmesano", 10)],
     ["Cuece la pasta.", "Mezcla con tomate y atún, queso por encima."], ["Gluten", "Pescado", "Lactosa"]),
    ("Pavo con calabacín y cuscús", "🦃", "CN", 15, [("Pavo, pechuga", 180), ("Calabacín", 200), ("Sémola de trigo, cruda", 60), (AOVE, 8)],
     ["Hidrata el cuscús con agua caliente 5 min.", "Pavo y calabacín a la plancha."], ["Gluten"]),
    ("Revuelto de champiñones y gambas", "🍤", "N", 10, [("Huevo de gallina fresco", 120), ("Champiñon", 150), ("Gamba", 80), (AOVE, 8), (PAN, 40)],
     ["Saltea champiñones y gambas.", "Añade los huevos batidos y remueve hasta cuajar."], ["Huevo", "Crustáceos", "Gluten"]),
    ("Crema de calabacín y tortilla", "🥣", "N", 25, [("Calabacín", 300), ("Patata, cruda", 100), ("Cebolla", 50), ("Huevo de gallina fresco", 120), (AOVE, 10)],
     ["Cuece calabacín, patata y cebolla 15 min y tritura.", "Tortilla francesa aparte."], ["Huevo"]),
    ("Arroz con pollo y verduras", "🍚", "C", 30, [("Arroz", 90), ("Pollo, pechuga, con piel, crudo", 150), ("Pimiento rojo", 60), ("Judía verde", 80), ("Tomate", 60), (AOVE, 10)],
     ["Sofríe pollo y verduras.", "Añade arroz y el doble de agua, 18 min."], []),
    ("Paella de marisco", "🥘", "C", 40, [("Arroz", 90), ("Gamba", 80), ("Mejillón", 100), ("Calamar", 80), ("Tomate", 50), (AOVE, 12)],
     ["Sofríe el marisco y el tomate.", "Arroz y caldo, 18 min sin remover."], ["Crustáceos", "Moluscos"]),
    ("Filete de ternera con patatas al horno", "🥩", "C", 30, [("Ternera, lomo, crudo", 160), ("Patata, cruda", 250), ("Pimiento rojo, crudo", 60), (AOVE, 10)],
     ["Patatas y pimiento al horno 25 min.", "Filete a la plancha al punto."], []),
    ("Lubina a la plancha con ensalada", "🐟", "N", 15, [("Lubina", 200), ("Lechuga", 80), ("Tomate", 100), (PAN, 40), (AOVE, 8)],
     ["Lubina a la plancha por la piel.", "Ensalada aliñada."], ["Pescado", "Gluten"]),
    ("Gazpacho con huevo y jamón", "🍅", "CN", 10, [("Gazpacho", 300), ("Huevo de gallina, hervido, duro", 55), ("Jamón serrano", 30), (PAN, 30)],
     ["Sirve el gazpacho frío con huevo y jamón picados."], ["Huevo", "Gluten"]),
    ("Ensalada de pasta con pavo", "🥗", "CN", 15, [("Pasta alimenticia, cruda", 80), ("Pavo, fiambre, bajo en grasa", 60), ("Tomate", 100), ("Maíz, en mazorca, congelado", 50), (AOVE, 8)],
     ["Cuece la pasta y enfríala.", "Mezcla con el resto y aliña."], ["Gluten"]),
    ("Huevos rotos ligeros con jamón", "🍳", "CN", 25, [("Patata, cruda", 200), ("Huevo de gallina fresco", 110), ("Jamón serrano", 40), (AOVE, 10)],
     ["Patatas en gajos al horno o airfryer 20 min.", "Huevos a la plancha y jamón por encima."], ["Huevo"]),
    ("Sepia a la plancha con arroz", "🦑", "CN", 20, [("Sepia", 200), ("Arroz", 70), ("Ajo", 5), (AOVE, 10)],
     ["Cuece el arroz.", "Sepia a la plancha con ajo y perejil."], ["Moluscos"]),
    ("Pisto con huevo", "🍳", "CN", 30, [("Calabacín", 150), ("Berenjena", 100), ("Pimiento rojo", 80), ("Tomate frito", 80), ("Huevo de gallina fresco", 60), (AOVE, 10), (PAN, 40)],
     ["Pocha las verduras en el aceite 20 min.", "Añade el tomate y un huevo encima, tapa 3 min."], ["Huevo", "Gluten"]),
    ("Bacalao con tomate y patata", "🐟", "CN", 25, [("Bacalao", 200), ("Tomate frito", 100), ("Pimiento rojo, crudo", 60), ("Patata, cruda", 150), (AOVE, 8)],
     ["Patata cocida.", "Bacalao en la salsa de tomate y pimiento 8 min."], ["Pescado"]),
    ("Alubias con verduras", "🥣", "C", 15, [("Alubia blanca, en conserva", 240), ("Zanahoria", 60), ("Pimiento rojo, crudo", 50), ("Cebolla", 50), (AOVE, 10)],
     ["Sofríe las verduras.", "Añade las alubias escurridas y un poco de agua, 10 min."], []),
    ("Tostadas con tomate y aceite", "🍞", "DR", 5, [(PAN, 80), ("Tomate", 60), (AOVE, 8)],
     ["Tuesta el pan, tomate rallado y aceite."], ["Gluten"]),
    ("Tostadas con tomate y jamón", "🍞", "DR", 5, [(PAN, 80), ("Tomate", 60), ("Jamón serrano", 30), (AOVE, 5)],
     ["Tostada con tomate rallado, aceite y jamón."], ["Gluten"]),
    ("Tostada de aguacate y huevo", "🥑", "DR", 10, [("Pan integral", 60), ("Aguacate", 60), ("Huevo de gallina fresco", 60)],
     ["Tuesta el pan, aguacate machacado y huevo a la plancha."], ["Gluten", "Huevo"]),
    ("Yogur con avena y plátano", "🥣", "DMR", 3, [("Yogur, desnatado, sabor natural", 250), ("Avena", 40), ("Plátano", 100)],
     ["Mezcla el yogur con la avena y el plátano en rodajas."], ["Lactosa", "Gluten"]),
    ("Porridge de avena con fresas", "🥣", "D", 8, [("Avena", 50), ("Leche, semidesnatada, pasteurizada", 250), ("Fresa", 100)],
     ["Cuece la avena en la leche 5 min y añade las fresas."], ["Gluten", "Lactosa"]),
    ("Café con leche y tostada con queso fresco", "☕", "D", 5, [("Leche, semidesnatada, pasteurizada", 200), ("Pan integral", 60), ("Queso fresco de burgos", 60)],
     ["Tuesta el pan y pon el queso fresco."], ["Lactosa", "Gluten"]),
    ("Tortitas de avena y claras", "🥞", "DR", 10, [("Avena", 50), ("Huevo de gallina, clara, cruda", 150), ("Plátano", 60), ("Miel", 10)],
     ["Tritura avena, claras y plátano.", "Haz las tortitas en sartén antiadherente y añade la miel."], ["Gluten", "Huevo"]),
    ("Bocadillo de pavo y queso", "🥪", "MR", 5, [(PAN, 80), ("Pavo, fiambre, bajo en grasa", 50), ("Queso edam", 20), ("Tomate", 40)],
     ["Monta el bocadillo."], ["Gluten", "Lactosa"]),
    ("Bocadillo de atún con pimiento", "🥪", "MR", 5, [(PAN, 80), ("Atún, al natural", 50), ("Pimiento rojo", 40)],
     ["Monta el bocadillo."], ["Gluten", "Pescado"]),
    ("Fruta con nueces", "🍎", "MR", 2, [("Manzana", 180), ("Nuez", 20)],
     ["Una pieza de fruta y un puñado de nueces."], ["Frutos secos"]),
    ("Yogur con nueces y miel", "🥛", "MR", 2, [("Yogur, desnatado, sabor natural", 125), ("Nuez", 15), ("Miel", 10)],
     ["Mezcla y listo."], ["Lactosa", "Frutos secos"]),
    ("Plátano con cacahuetes", "🍌", "MR", 2, [("Plátano", 120), ("Cacahuete, tostado", 20)],
     ["Plátano y un puñado de cacahuetes."], ["Cacahuete"]),
    ("Sándwich mixto", "🥪", "DMRN", 5, [("Pan blanco, de molde", 60), ("Jamón cocido", 40), ("Queso edam", 30)],
     ["Monta y tuesta en sartén o sandwichera."], ["Gluten", "Lactosa"]),
    ("Merluza en salsa verde con arroz", "🐟", "C", 25, [("Merluza fresca", 200), ("Arroz", 70), ("Guisante, congelado, crudo", 50), ("Ajo", 5), (AOVE, 10)],
     ["Dora el ajo, añade perejil, una cucharadita de harina y agua.", "Cuece la merluza y los guisantes en la salsa 6 min. Arroz blanco aparte."], ["Pescado"]),
    ("Calabacín relleno de carne", "🥒", "CN", 35, [("Calabacín", 300), ("Carne picada", 120), ("Tomate frito", 60), ("Queso edam", 20)],
     ["Vacía el calabacín y sofríe la carne con la pulpa y el tomate.", "Rellena, queso por encima y horno 15 min."], ["Lactosa"]),
    ("Sopa de fideos con huevo", "🍜", "N", 15, [("Pasta alimenticia, cruda", 40), ("Huevo de gallina, hervido, duro", 55), ("Jamón serrano", 20)],
     ["Cuece los fideos en caldo 5 min.", "Añade huevo y jamón picados."], ["Gluten", "Huevo"]),
    ("Pechuga de pollo con ensalada y patata", "🍗", "N", 20, [("Pollo, pechuga, plancha", 150), ("Lechuga", 80), ("Tomate", 100), ("Patata, cruda", 150), (AOVE, 8)],
     ["Patata cocida o al microondas 8 min.", "Pollo a la plancha y ensalada aliñada."], []),
]


def buscar(c, patron):
    sql = "select nombre,kcal_100g,proteinas_100g,hidratos_100g,grasas_100g from alimentos where tipo='generico' and "
    return c.execute(sql + "nombre=?", (patron,)).fetchone() or \
        c.execute(sql + "nombre like ? order by length(nombre) limit 1", (patron + '%',)).fetchone()


def main():
    c = sqlite3.connect(BD)
    salida, faltan = [], []
    for nombre, emoji, comidas, minutos, ings, pasos, alerg in R:
        lista = []
        for patron, g in ings:
            f = buscar(c, patron)
            if not f:
                faltan.append(f"{nombre}: {patron}")
                continue
            lista.append({"n": f[0], "g": g, "k": f[1] or 0, "p": f[2] or 0, "h": f[3] or 0, "gr": f[4] or 0})
        salida.append({"nombre": nombre, "emoji": emoji, "comidas": comidas, "min": minutos, "ing": lista, "pasos": pasos, "alergenos": alerg})
    if faltan:
        print("SIN ENCONTRAR:\n" + "\n".join(faltan))
        sys.exit(1)
    for r in salida:
        k = sum(i["k"] * i["g"] / 100 for i in r["ing"])
        p = sum(i["p"] * i["g"] / 100 for i in r["ing"])
        print(f'{k:5.0f} kcal P{p:4.0f}  {r["nombre"]}: ' + ", ".join(f'{i["n"]} ({i["k"]:.0f})' for i in r["ing"]))
    json.dump(salida, open(SALIDA, 'w', encoding='utf-8'), ensure_ascii=False, separators=(',', ':'))


main()
