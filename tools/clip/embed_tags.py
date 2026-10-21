"""Genera app/src/main/assets/clip/tag_embeddings.json: vector de texto CLIP de cada etiqueta.

Uso: python embed_tags.py <TagVocabulary.kt> <carpeta con text_model_quantized.onnx y tokenizer.json> <salida.json>
Modelo: https://huggingface.co/Xenova/clip-vit-base-patch32 (onnx/text_model_quantized.onnx).
"""
import json, re, sys
import numpy as np, onnxruntime as ort
from tokenizers import Tokenizer

vocab_src, model_dir, out = sys.argv[1], sys.argv[2], sys.argv[3]
ids = re.findall(r'(?:cat|style|color|fit)\("([a-z_0-9]+)"', open(vocab_src).read())

EN = {
 "camiseta":"t-shirt","tirantes":"tank top","camisa":"button-up shirt","polo":"polo shirt","sudadera":"sweatshirt or hoodie",
 "jersey":"knit sweater","chaqueta":"jacket","cazadora":"bomber jacket","abrigo":"coat","chaleco":"vest","vaqueros":"pair of jeans",
 "pantalon":"pair of trousers","chino":"pair of chino pants","cargo":"pair of cargo pants","shorts":"pair of shorts",
 "chandal":"pair of track pants","banador":"pair of swim shorts","traje":"suit","zapatillas":"pair of sneakers","botas":"pair of boots",
 "zapatos":"pair of leather shoes","sandalias":"pair of sandals","gorra":"cap or hat","mochila":"backpack or bag",
 "streetwear":"streetwear","skate":"skater","minimalista":"minimalist","vintage":"vintage","y2k":"y2k","old_money":"old money preppy elegant",
 "deportivo":"sporty athletic","formal":"formal","casual":"casual","grunge":"grunge","preppy":"preppy","workwear":"workwear",
 "techwear":"techwear outdoor","militar":"military","surf":"surf",
 "negro":"black","blanco":"white","gris":"grey","beige":"beige","marron":"brown","tonos_tierra":"earth-toned olive khaki",
 "azul_marino":"navy blue","azul_claro":"light blue","azul":"blue","verde":"green","rojo":"red","rosa":"pink","amarillo":"yellow",
 "naranja":"orange","morado":"purple","estampado":"patterned printed",
 "oversize":"oversized","slim":"slim fit","recto":"straight fit","ancho":"baggy wide","cropped":"cropped","denim":"denim",
 "punto":"knitted wool","cuero":"leather","lino":"linen","pana":"corduroy","algodon":"cotton","polar":"fleece","acolchado":"puffer padded",
 "desgastado":"washed distressed","seda":"silk satin",
}
GROUP = {}
for kind, i in re.findall(r'(cat|style|color|fit)\("([a-z_0-9]+)"', open(vocab_src).read()):
    GROUP[i] = kind
missing = [i for i in ids if i not in EN]
assert not missing, missing

def prompt(i):
    k, en = GROUP[i], EN[i]
    if k == "cat": return f"a photo of a {en}" if not en.startswith("pair") else f"a photo of a {en}"
    if k == "style": return f"a photo of {en} style clothing"
    if k == "color": return f"a photo of {en} clothing"
    return f"a photo of {en} clothing"

tok = Tokenizer.from_file(f"{model_dir}/tokenizer.json")
tok.enable_padding(length=77, pad_id=49407); tok.enable_truncation(77)
sess = ort.InferenceSession(f"{model_dir}/text_model_quantized.onnx")
print([ (i.name, i.shape) for i in sess.get_inputs()], [o.name for o in sess.get_outputs()], file=sys.stderr)
enc = tok.encode_batch([prompt(i) for i in ids])
input_ids = np.array([e.ids for e in enc], dtype=np.int64)
attn = np.array([e.attention_mask for e in enc], dtype=np.int64)
feeds = {"input_ids": input_ids}
if "attention_mask" in [i.name for i in sess.get_inputs()]: feeds["attention_mask"] = attn
outs = dict(zip([o.name for o in sess.get_outputs()], sess.run(None, feeds)))
emb = outs.get("text_embeds")
emb = emb / np.linalg.norm(emb, axis=1, keepdims=True)
json.dump({"model": "Xenova/clip-vit-base-patch32", "dim": int(emb.shape[1]),
           "tags": {i: [round(float(x), 5) for x in v] for i, v in zip(ids, emb)}}, open(out, "w"))
print(len(ids), "tags ->", out, file=sys.stderr)
