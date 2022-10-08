package me.dio.sacola.service.impl;

import lombok.RequiredArgsConstructor;
import me.dio.sacola.enumeration.FormaPagamento;
import me.dio.sacola.model.Item;
import me.dio.sacola.model.Restaurante;
import me.dio.sacola.model.Sacola;
import me.dio.sacola.repository.ItemRepository;
import me.dio.sacola.repository.ProdutoRepository;
import me.dio.sacola.repository.SacolaRepository;
import me.dio.sacola.resource.dto.ItemDto;
import me.dio.sacola.service.SacolaService;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SacolaServiceImpl implements SacolaService {

    private final SacolaRepository sacolaRepository;
    private final ProdutoRepository produtoRepository;
    private final ItemRepository itemRepository;

    @Override
    public Item incluirItemNaSacola(ItemDto itemDto) {
        Sacola sacola = verSacola(itemDto.getIdSacola());

        // Não pode inserir itens se a sacola estiver fechada
        if(sacola.isFechada()){
            throw new RuntimeException("Essa sacola está fechada!");
        }

        // Cria um novo item
        Item itemParaSerInserido = Item.builder()
            .quantidade(itemDto.getQuantidade())
            .sacola(sacola)
            .produto(produtoRepository.findById(itemDto.getProdutoId()).orElseThrow(
                    () -> {
                        throw new RuntimeException("Esse produto não existe");
                    }
            ))
            .build();

        List<Item> itensDaSacola = sacola.getItens();

        // Adiciona o item na sacola, caso ela esteja vazia
        if(itensDaSacola.isEmpty()){
            itensDaSacola.add(itemParaSerInserido);
        }

        // Impede de adicionar produtos de restaurantes diferentes na sacola
        else {
            Restaurante restauranteAtual = itensDaSacola.get(0).getProduto().getRestaurante();
            Restaurante restauranteNovo = itemParaSerInserido.getProduto().getRestaurante();

            if(restauranteAtual.equals(restauranteNovo)){
                itensDaSacola.add(itemParaSerInserido);
            } else {
                throw new RuntimeException("Não é possível adicionar produtos de restaurantes diferentes!");
            }
        }

        // Atualiza o valor total da sacola
        List<Double> valorDosItens = new ArrayList<>();
        for (Item itemSacola : itensDaSacola) {
            Double valorTotalItem = itemSacola.getProduto().getValorUnitario() * itemSacola.getQuantidade();
            valorDosItens.add(valorTotalItem);
        }

        Double valorTotalSacola = valorDosItens.stream().reduce(0.0, Double::sum);
        sacola.setValorTotal(valorTotalSacola);

        sacolaRepository.save(sacola);

        return itemRepository.save(itemParaSerInserido);
    }

    public Item removerItemDaSacola(Long sacolaId, Long itemId){

        Sacola sacola = verSacola(sacolaId);

        List<Item> itensDaSacola = sacola.getItens();

        // Não tem itens para remover, a sacola está vazia
        if(itensDaSacola.isEmpty()){
            throw new RuntimeException("Não tem itens para remover, a sacola está vazia!");
        }

        // Esse item não existe nessa sacola
        Item itemParaSerRemovido = null;
        boolean naoExiste = true;
        for (Item itemSacola : itensDaSacola){
            if (itemSacola.getId().equals(itemId)) {
                itemParaSerRemovido = itemSacola;
                naoExiste = false;
                break;
            }
        }
        if(naoExiste){
            throw new RuntimeException("Esse item não existe nessa sacola!");
        }

        // Não pode remover itens se a sacola estiver fechada
        if(sacola.isFechada()){
            throw new RuntimeException("Essa sacola está fechada!");
        }

        // Remove o item da sacola
        itensDaSacola.remove(itemParaSerRemovido);

        // Atualiza a sacola
        sacola.setItens(itensDaSacola);
        sacolaRepository.save(sacola);

        // Retorna o item removido
        itemRepository.delete(itemParaSerRemovido);
        return itemParaSerRemovido;
    }

    @Override
    public Sacola verSacola(Long id) {
        return sacolaRepository.findById(id).orElseThrow(
            () -> {
                throw new RuntimeException("Essa sacola não existe!");
            }
        );
    }

    @Override
    public Sacola fecharSacola(Long id, int numeroFormaPagamento) {
        Sacola sacola = verSacola(id);

        if(sacola.getItens().isEmpty()){
            throw new RuntimeException("Inclua itens na sacola");
        }

        FormaPagamento formaPagamento = numeroFormaPagamento == 0 ? FormaPagamento.DINHEIRO : FormaPagamento.MAQUINETA;

        sacola.setFormaPagamento(formaPagamento);
        sacola.setFechada(true);

        return sacolaRepository.save(sacola);
    }
}
