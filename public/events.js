let count = 0;
const counterE1 = document.querySelector('[data-id="counter"]');
const incE1 = document.querySelector('[data-action="inc"]');

incE1.addEventListener('click', () => {
    count++;
    counterE1.textContent = `${count}`;
});