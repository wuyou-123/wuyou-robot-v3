import json

import requests

from pypinyin import pinyin
from concurrent.futures import ThreadPoolExecutor

thread_pool = ThreadPoolExecutor(max_workers=30)


def test(d):
    text = requests.get("https://chengyu.aies.cn/cha_" + d['word'] + ".htm").text
    if text.__contains__('<span class="pinyin">'):
        pinyin = text[text.index('<span class="pinyin">') + 21: text.index('</span><span>')]
        if len(pinyin.replace('，', '').split(" ")) == len(d['word'].replace('，', '')):
            d['pinyin'] = pinyin
            print('更正了', d)
    else:
        row_data.pop(row_data.index(d))
        print('删除了', d)


path = "idiom.json"
newPath = "idiom.json"
# 读取文件数据
f = open(path, "r", encoding='utf-8')
row_data = json.load(f)
# print(row_data)
for d in row_data:
    p = pinyin(d['word'], heteronym=False)
    arr = []
    for data in p:
        arr.append(data[0])
    if " ".join(arr).replace(" ， ", "，") != d['pinyin']:
        thread_pool.submit(test, d)
        # open(newPath, "w", encoding='utf-8').write(str(row_data))
thread_pool.shutdown(wait=True)
print(row_data)
open(newPath, "w", encoding='utf-8').write(str(row_data))
