"""Check complete native translations, Android format placeholders and locale declarations."""
from pathlib import Path
import re
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parent.parent
ANDROID = '{http://schemas.android.com/apk/res/android}'
LOCALES = {
    'en':'values', 'tr':'values-tr', 'hi':'values-hi', 'ja':'values-ja',
    'pt':'values-pt', 'pt-BR':'values-pt-rBR', 'ru':'values-ru', 'es':'values-es',
    'ar':'values-ar', 'fr':'values-fr', 'de':'values-de', 'id':'values-in',
    'zh-Hans':'values-b+zh+Hans-v21', 'zh-Hant':'values-b+zh+Hant-v21',
    'ko':'values-ko', 'it':'values-it', 'bn':'values-bn', 'ur':'values-ur', 'fa':'values-fa',
}

def read_strings(path):
    elements = ET.parse(path).getroot().findall('string')
    names = [element.attrib['name'] for element in elements]
    assert len(names) == len(set(names)), f'{path}: duplicate resource keys'
    result = {element.attrib['name']: ''.join(element.itertext()) for element in elements}
    assert all(value.strip() for value in result.values()), f'{path}: empty translations'
    return result

def validate():
    for module in ('app',):
        res = ROOT/module/'src/main/res'
        base = read_strings(res/'values/strings.xml')
        declared = [element.attrib[ANDROID+'name'] for element in ET.parse(res/'xml/locales_config.xml').getroot()]
        assert len(declared) == len(set(declared)) and set(declared) == set(LOCALES), f'{module}: locale declaration mismatch'
        for folder in set(LOCALES.values()) | {'values-zh','values-zh-rTW','values-zh-rHK'}:
            translated = read_strings(res/folder/'strings.xml')
            assert translated.keys() == base.keys(), f'{module}/{folder}: missing/extra keys: {translated.keys() ^ base.keys()}'
            for key, original in base.items():
                placeholders = lambda value: sorted(re.findall(r'%(?:\d+\$)?[a-zA-Z]', value))
                assert placeholders(original) == placeholders(translated[key]), f'{module}/{folder}/{key}: invalid placeholders'
        for first, second in (('values-zh','values-b+zh+Hans-v21'), ('values-zh-rTW','values-b+zh+Hant-v21'), ('values-zh-rHK','values-b+zh+Hant-v21')):
            assert read_strings(res/first/'strings.xml') == read_strings(res/second/'strings.xml'), f'{module}: Chinese fallback differs'
        print(f'{module}: {len(LOCALES)} locale variants; {len(base)} strings each; placeholders and Chinese fallbacks valid.')

if __name__ == '__main__': validate()
