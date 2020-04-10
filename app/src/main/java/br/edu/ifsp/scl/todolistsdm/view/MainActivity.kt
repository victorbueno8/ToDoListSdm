package br.edu.ifsp.scl.todolistsdm.view

import android.app.Activity
import android.content.Intent
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import androidx.room.Room
import br.edu.ifsp.scl.todolistsdm.R
import br.edu.ifsp.scl.todolistsdm.adapter.ListaTarefasAdapter
import br.edu.ifsp.scl.todolistsdm.model.database.ToDoListDatabase
import br.edu.ifsp.scl.todolistsdm.model.entity.Tarefa
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.celula_lista_tarefas.view.*
import kotlinx.android.synthetic.main.conteudo_principal.*
import kotlinx.android.synthetic.main.toolbar.*
import splitties.alertdialog.alertDialog
import splitties.alertdialog.cancelButton
import splitties.alertdialog.message
import splitties.alertdialog.okButton
import splitties.toast.toast
import br.edu.ifsp.scl.todolistsdm.model.database.ToDoListDatabase.Constantes as Constantes1

class MainActivity : AppCompatActivity() {
    object Constantes {
        const val TAREFA_ACTIVITY_REQUEST_CODE = 0
        const val TAREFA_EXTRA = "TAREFA_EXTRA"
    }

    private lateinit var listaTarefasAdapter: ListaTarefasAdapter
    private lateinit var listaTarefas: MutableList<Tarefa>

    private lateinit var toDoListDatabase: ToDoListDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toolbarTb.title = getString(R.string.app_name)
        setSupportActionBar(toolbarTb)

        /*
            Buscar referência com fonte de dados
         */
        toDoListDatabase = Room.databaseBuilder(
            this,
            ToDoListDatabase::class.java,
            ToDoListDatabase.Constantes.DB_NAME
        ).build()

        /*
        Recuperar tarefas da fonte de dados e passar para o adaptador do ListView
         */
        val toDoList = CarregarTarefasAT().execute()
        listaTarefasAdapter =
            ListaTarefasAdapter(
                this,
                toDoList.get().toMutableList()
            )
        conteudoLv.adapter = listaTarefasAdapter

        /* Adaptador do ListView */
        listaTarefas = toDoList.get().toMutableList()

        /* Menu de contexto */
        conteudoLv.setOnItemClickListener { _, view, position, _ ->
            val tarefaClicada = listaTarefas[position]
            tarefaClicada.checado = if (tarefaClicada.checado == 1) 0 else 1

            view.checadoTarefaCb.isChecked = tarefaClicada.checado == 1

            /*
            Atualizar atributo checado na fonte de dados
             */
            AtualizarTarefaAT().execute(tarefaClicada)
        }

        registerForContextMenu(conteudoLv)

        /* Botão de ação flutuante para adicionar nova tarefa */
        novaTarefaFab.setOnClickListener {
            /* Abrir TarefaActivity para inserção */
            val tarefaIntent = Intent(this, TarefaActivity::class.java)
            startActivityForResult(tarefaIntent, Constantes.TAREFA_ACTIVITY_REQUEST_CODE)
        }

    }


    private inner class CarregarTarefasAT: AsyncTask<Unit, Unit, List<Tarefa>>() {

        override fun doInBackground(vararg params: Unit?): List<Tarefa> {
            return toDoListDatabase.getTarefaDao().recuperarTarefas()
        }

    }

    private inner class AtualizarTarefaAT: AsyncTask<Tarefa, Unit, Unit>() {

        override fun doInBackground(vararg params: Tarefa?) {
            return toDoListDatabase.getTarefaDao().atualizarTarefa(params[0]!!)
        }

    }



    override fun onCreateContextMenu(
        menu: ContextMenu?,
        v: View?,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        menuInflater.inflate(R.menu.menu_context_main, menu)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val infoMi = item.menuInfo as AdapterView.AdapterContextMenuInfo
        val tarefaClicada = listaTarefasAdapter.getItem(infoMi.position)

        when (item.itemId) {
            R.id.editarTarefaMi -> {
                /* Abrir TarefaActivity para edição, enviar tarefa como parâmetro */
                val editarTarefaIntent = Intent(this, TarefaActivity::class.java)
                editarTarefaIntent.putExtra(Constantes.TAREFA_EXTRA, tarefaClicada)
                startActivityForResult(editarTarefaIntent, Constantes.TAREFA_ACTIVITY_REQUEST_CODE)
            }
            R.id.removerTarefaMi -> {
                alertDialog {
                    message = getString(R.string.excluir_tarefa)
                    okButton {
                        /*
                        Remover tarefa da fonte de dados
                         */
                        RemoverTarefaAT().execute(tarefaClicada)

                        /* Remover tarefa do adaptador do ListView */
                        listaTarefasAdapter.remove(tarefaClicada)
                        toast(getString(R.string.tarefa_removida))
                    }
                    cancelButton {
                        /* Ação cancelada pelo usuário, nada necessário */
                    }
                }.show()
            }
        }
        return true
    }

    private inner class RemoverTarefaAT: AsyncTask<Tarefa, Unit, Unit>() {

        override fun doInBackground(vararg params: Tarefa?) {
            toDoListDatabase.getTarefaDao().removerTarefa(params[0]!!)
        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.limparTudoMi) {
            alertDialog {
                message = getString(R.string.excluir_todas_tarefas)
                okButton {
                    /*
                    Remover TODAS as tarefas da fonte de dados
                     */
                    RemoverTarefasAT().execute()

                    /* Remover TODAS as tarefas do adaptador do ListView */
                    listaTarefas.clear()
                    listaTarefasAdapter.notifyDataSetChanged()
                    listaTarefasAdapter.clear()
                    toast(getString(R.string.tarefas_removidas))
                }
                cancelButton {
                    /* Ação cancelada pelo usuário, nada necessário */
                }
            }.show()
        }
        return true
    }

    private inner class RemoverTarefasAT: AsyncTask<Unit, Unit, Unit>() {

        override fun doInBackground(vararg params: Unit?) {
            toDoListDatabase.getTarefaDao().removerTodasTarefas()
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Constantes.TAREFA_ACTIVITY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            /* Pegando tarefa retornada (se existe) pela TarefaActivity */
            val tarefaRetorno = data?.getParcelableExtra<Tarefa>(Constantes.TAREFA_EXTRA)

            if (tarefaRetorno != null) {
                /* Verificando se tarefa já existe no adaptador do ListView */
                val tarefaExistente = listaTarefasAdapter.getTarefaById(tarefaRetorno.id)
                if (tarefaExistente != null) {
                    /* Atualiza tarefa existente */
                    val indiceTarefaExistente = listaTarefas.indexOf(tarefaExistente)

                    /* Remover e inserir força atualização do ListView */
                    listaTarefasAdapter.remove(tarefaExistente)
                    listaTarefasAdapter.insert(tarefaRetorno, indiceTarefaExistente)
                }
                else {
                    /* Tarefa não existente, então adiciona */
                    listaTarefasAdapter.add(tarefaRetorno)
                }
            }
        }
    }
}
