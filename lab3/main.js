const getRandomUserButton = document.getElementById('getRandomUserButton');
const userData = document.getElementById('userData');
const userList = document.getElementById('userList');

getRandomUserButton.addEventListener('click', () => {
    fetch('https://randomuser.me/api')
        .then(response => response.json())
        .then(data => {
            const user = data.results[0];
            const name = `${user.name.first} ${user.name.last}`;
            const email = user.email;
            const city = user.location.city;
            const postcode = user.location.postcode;
            const image = user.picture.large;

            const listItem = document.createElement('li');
            listItem.innerHTML = `
                <strong>Ім'я:</strong> ${name}<br>
                <strong>Email:</strong> ${email}<br>
                <strong>Місто:</strong> ${city}<br>
                <strong>Поштовий індекс:</strong> ${postcode}<br>
                <strong>Зображення:</strong><br>
                <img src="${image}" alt="Фото користувача">
            `;

            userList.appendChild(listItem);

            userData.style.display = 'block';
        })
        .catch(error => {
            console.error('Помилка при отриманні даних:', error);
        });
});