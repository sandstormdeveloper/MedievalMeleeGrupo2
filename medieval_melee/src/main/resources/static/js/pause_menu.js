// Clase PauseMenuScene que representa el menú principal del juego
class PauseMenuScene extends Phaser.Scene {
    // Constructor de la escena, define la clave de la escena
    constructor() {
        super({ key: 'PauseMenuScene' });
    }

    // Método preload: carga los recursos necesarios para esta escena
    preload() {
        this.load.image('fondo_pausa', 'assets/pausa.png');               // Fondo del menú
        this.load.image('play', 'assets/play.png');               // Botón de "Jugar"
        this.load.image('play_hover', 'assets/play_hover.png');   // Botón de "Jugar" en hover
        this.load.image('exit', 'assets/exit.png');         // Botón de "Salir"
        this.load.image('exit_hover', 'assets/exit_hover.png'); // Botón de "Salir" en hover

        this.load.audio('menu_music', 'assets/menu.mp3');
    }

    // Método create: configura la escena y sus elementos
    create() {
        // Agrega la imagen de fondo y el título en posiciones específicas
        this.add.image(640, 360, 'fondo_pausa');  // Imagen del fondo

        // Botón de "Jugar"
        var start_button = this.add.image(640, 390, 'play')
            .setInteractive() // Hace el botón interactivo
            .on('pointerover', () => {
                // Cambia a la textura de hover cuando el mouse pasa sobre el botón
                start_button.setTexture('play_hover');
            })
            .on('pointerout', () => {
                // Vuelve a la textura normal cuando el mouse sale del botón
                start_button.setTexture('play');
            })
            .on('pointerdown', () => {
                this.scene.stop(); // Detiene la escena de pausa
                this.scene.resume('GameScene'); // Reanuda la escena principal
            });

        // Botón de "Créditos"
        var exit_button = this.add.image(640, 450, 'exit')
            .setInteractive() // Hace el botón interactivo
            .on('pointerover', () => {
                // Cambia a la textura de hover cuando el mouse pasa sobre el botón
                exit_button.setTexture('exit_hover');
            })
            .on('pointerout', () => {
                // Vuelve a la textura normal cuando el mouse sale del botón
                exit_button.setTexture('exit');
            })
            .on('pointerdown', () => {
                // Al hacer clic, inicia un fade-out y cambia a la escena de créditos
                this.cameras.main.fadeOut(500, 0, 0, 0);

                // Espera a que el fade-out termine antes de iniciar la nueva escena
                this.cameras.main.once('camerafadeoutcomplete', () => {
                    this.scene.stop('GameScene'); // Detiene la escena principal
                    this.scene.start('MainMenuScene'); // Vuelve al menú principal
                    this.game.music.stop();
                    this.game.music = this.sound.add('menu_music', { loop: true });
                    this.game.music.play();
                });
            });
    }

    // Método update: se ejecuta en cada frame, puede usarse para lógica del juego (vacío aquí)
    update(time, delta) {
        // Sin implementación adicional en este ejemplo
    }
}