import urllib.request
import os
import zipfile

os.makedirs('app/src/main/res/font', exist_ok=True)

# Plus Jakarta Sans
urllib.request.urlretrieve('https://github.com/google/fonts/raw/main/ofl/plusjakartasans/PlusJakartaSans%5Bwght%5D.ttf', 'app/src/main/res/font/plus_jakarta_sans.ttf')

# Be Vietnam Pro
urllib.request.urlretrieve('https://github.com/google/fonts/raw/main/ofl/bevietnampro/BeVietnamPro-Regular.ttf', 'app/src/main/res/font/be_vietnam_pro_regular.ttf')
urllib.request.urlretrieve('https://github.com/google/fonts/raw/main/ofl/bevietnampro/BeVietnamPro-Bold.ttf', 'app/src/main/res/font/be_vietnam_pro_bold.ttf')
urllib.request.urlretrieve('https://github.com/google/fonts/raw/main/ofl/bevietnampro/BeVietnamPro-Medium.ttf', 'app/src/main/res/font/be_vietnam_pro_medium.ttf')
urllib.request.urlretrieve('https://github.com/google/fonts/raw/main/ofl/bevietnampro/BeVietnamPro-SemiBold.ttf', 'app/src/main/res/font/be_vietnam_pro_semi_bold.ttf')

print("Fonts downloaded successfully.")
