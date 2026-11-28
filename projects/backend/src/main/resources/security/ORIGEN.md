# `contrasenas-comunes.txt`

Lista de contraseñas muy usadas en filtraciones, para rechazarlas al crear o
cambiar una contraseña. La usa `ContrasenasComunes`.

| Dato | Valor |
| --- | --- |
| Origen | [danielmiessler/SecLists](https://github.com/danielmiessler/SecLists), `Passwords/Common-Credentials/100k-most-used-passwords-NCSC.txt` |
| Commit | `1a7bb9127eca9e6ff2fc0301c597fe6e16a0cb56` |
| SHA-256 del original | `c2e5696882c603b76bb67a47ee970897e5a76fc4c3f5547abe3d0ca340c576e0` |
| Descargada | 2026-11-28 |

## Cómo se preparó

Del original se dejaron las líneas de 8 caracteres o más y 72 bytes o menos
(fuera de ese rango la contraseña ya se rechaza), en minúscula, sin repetidas y
ordenadas.

## Licencia

MIT License

Copyright (c) 2018 Daniel Miessler

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
